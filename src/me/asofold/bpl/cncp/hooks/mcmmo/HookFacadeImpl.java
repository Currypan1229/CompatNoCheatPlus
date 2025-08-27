package me.asofold.bpl.cncp.hooks.mcmmo;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.IViolationInfo;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.Folia;
import fr.neatmonster.nocheatplus.hooks.NCPHook;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties.ToolProps;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties.ToolType;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import me.asofold.bpl.cncp.BukkitCompatNoCheatPlus;
import me.asofold.bpl.cncp.hooks.generic.ExemptionManager;
import me.asofold.bpl.cncp.hooks.generic.HookInstaBreak;
import me.asofold.bpl.cncp.hooks.mcmmo.HookmcMMO.HookFacade;
import me.asofold.bpl.cncp.utils.ActionFrequency;
import me.asofold.bpl.cncp.utils.TickTask2;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("deprecation")
public class HookFacadeImpl implements HookFacade, NCPHook {


    protected final ExemptionManager exMan = new ExemptionManager();

    /** Normal click per block skills. */
    protected final CheckType[] exemptBreakNormal = new CheckType[]{
            CheckType.BLOCKBREAK_FASTBREAK, CheckType.BLOCKBREAK_FREQUENCY,
            CheckType.BLOCKBREAK_NOSWING,
            CheckType.BLOCKBREAK_WRONGBLOCK, // Not optimal but ok.
    };


    protected final CheckType[] exemptBreakMany = new CheckType[]{
            CheckType.BLOCKBREAK, CheckType.COMBINED_IMPROBABLE,
    };

    /** Fighting damage of effects such as bleeding or area (potentially). */
    protected final CheckType[] exemptFightEffect = new CheckType[]{
            CheckType.FIGHT_SPEED, CheckType.FIGHT_DIRECTION,
            CheckType.FIGHT_ANGLE, CheckType.FIGHT_NOSWING,
            CheckType.FIGHT_REACH, CheckType.COMBINED_IMPROBABLE,
    };

    // Presets for after failure exemption.
    protected final Map<CheckType, Integer> cancelChecksBlockBreak = new HashMap<>();
    //	protected final Map<CheckType, Integer> cancelChecksBlockDamage = new HashMap<CheckType, Integer>();
    //	protected final Map<CheckType, Integer> cancelChecksDamage = new HashMap<CheckType, Integer>();

    protected final boolean useInstaBreakHook;
    protected final int clicksPerSecond;
    protected final Map<CheckType, Integer> cancelChecks = new HashMap<>();
    /**
     * Last block breaking time
     */
    protected final Map<String, ActionFrequency> lastBreak = new HashMap<>(50);
    protected String cancel = null;
    protected long cancelTicks = 0;
    /** Counter for nested events to cancel break counting. */
    protected int breakCancel = 0;

    protected int lastBreakAddCount = 0;
    protected long lastBreakCleanup = 0;

    public HookFacadeImpl(final boolean useInstaBreakHook, final int clicksPerSecond) {
        this.useInstaBreakHook = useInstaBreakHook;
        this.clicksPerSecond = clicksPerSecond;
        this.cancelChecksBlockBreak.put(CheckType.BLOCKBREAK_NOSWING, 1);
        this.cancelChecksBlockBreak.put(CheckType.BLOCKBREAK_FASTBREAK, 1);
        //		
        //		cancelChecksBlockDamage.put(CheckType.BLOCKBREAK_FASTBREAK, 1);
        //		
        //		cancelChecksDamage.put(CheckType.FIGHT_ANGLE, 1);
        //		cancelChecksDamage.put(CheckType.FIGHT_SPEED, 1);
    }

    @Override
    public String getHookName() {
        return "mcMMO(cncp)";
    }

    @Override
    public String getHookVersion() {
        return "2.3";
    }

    @Override
    public final boolean onCheckFailure(final CheckType checkType, final Player player, final IViolationInfo info) {
        //		System.out.println(player.getName() + " -> " + checkType + "---------------------------");
        // Somewhat generic canceling mechanism (within the same tick).
        // Might later fail, if block break event gets scheduled after block damage having set insta break, instead
        // of letting them follow directly.
        if (this.cancel == null) {
            return false;
        }

        final String name = player.getName();
        if (this.cancel.equals(name)) {

            if (player.getTicksLived() != this.cancelTicks) {
                this.cancel = null;
            } else {
                final Integer n = this.cancelChecks.get(checkType);
                if (n == null) {
                    return false;
                } else if (n > 0) {
                    if (n == 1) this.cancelChecks.remove(checkType);
                    else this.cancelChecks.put(checkType, n - 1);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public final void damageLowest(final Player player) {
        //		System.out.println("damage lowest");
        //		setPlayer(player, cancelChecksDamage);
        this.addExemption(player, this.exemptFightEffect);
    }

    public void addExemption(final Player player, final CheckType[] types) {
        for (final CheckType type : types) {
            this.exMan.addExemption(player, type);
            TickTask2.addUnexemptions(player, types);
        }
    }

    @Override
    public void damageMonitor(final Player player) {
        //		System.out.println("damage monitor");
        this.removeExemption(player, this.exemptFightEffect);
    }

    public void removeExemption(final Player player, final CheckType[] types) {
        for (final CheckType type : types) {
            this.exMan.removeExemption(player, type);
        }
    }

    @Override
    public final void blockDamageLowest(final Player player) {
        //		System.out.println("block damage lowest");
        //		setPlayer(player, cancelChecksBlockDamage);
        if (this.getToolProps(Bridge1_9.getItemInMainHand(player)).toolType == ToolType.AXE)
            this.addExemption(player, this.exemptBreakMany);
        else this.addExemption(player, this.exemptBreakNormal);
    }

    public ToolProps getToolProps(final ItemStack stack) {
        if (stack == null) return BlockProperties.noTool;
        else return BlockProperties.getToolProps(stack);
    }

    @Override
    public void blockDamageMonitor(final Player player) {
        //		System.out.println("block damage monitor");
        if (this.getToolProps(player.getItemInHand()).toolType == ToolType.AXE)
            this.addExemption(player, this.exemptBreakMany);
        else this.removeExemption(player, this.exemptBreakNormal);
    }

    @Override
    public final boolean blockBreakLowest(final Player player) {
        //		System.out.println("block break lowest");
        final boolean isAxe = this.getToolProps(Bridge1_9.getItemInMainHand(player)).toolType == ToolType.AXE;
        if (this.breakCancel > 0) {
            this.breakCancel++;
            return true;
        }
        final String name = player.getName();
        ActionFrequency freq = this.lastBreak.get(name);
        final long now = System.currentTimeMillis();
        if (freq == null) {
            freq = new ActionFrequency(3, 333);
            freq.add(now, 1f);
            this.lastBreak.put(name, freq);
            this.lastBreakAddCount++;
            if (this.lastBreakAddCount > 100) {
                this.lastBreakAddCount = 0;
                this.cleanupLastBreaks();
            }
        } else if (!isAxe) {
            freq.add(now, 1f);
            if (freq.score(1f) > (float) this.clicksPerSecond) {
                this.breakCancel++;
                return true;
            }
        }

        this.addExemption(player, this.exemptBreakNormal);
        if (this.useInstaBreakHook) {
            HookInstaBreak.addExemptNext(this.exemptBreakNormal);
            TickTask2.addUnexemptions(player, this.exemptBreakNormal);
        } else if (!isAxe) {
            this.setPlayer(player, this.cancelChecksBlockBreak);
            Folia.runSyncTask(BukkitCompatNoCheatPlus.getInstance(), (arg) -> DataManager.removeData(player.getName()
                    , CheckType.BLOCKBREAK_FASTBREAK));
        }
        return false;
    }

    private void setPlayer(final Player player, final Map<CheckType, Integer> cancelChecks) {
        this.cancel = player.getName();
        this.cancelTicks = player.getTicksLived();
        this.cancelChecks.clear();
        this.cancelChecks.putAll(cancelChecks);
    }

    protected void cleanupLastBreaks() {
        final long ts = System.currentTimeMillis();
        if (ts - this.lastBreakCleanup < 30000 && ts > this.lastBreakCleanup) return;
        this.lastBreakCleanup = ts;
        final List<String> rem = new LinkedList<>();
        if (ts >= this.lastBreakCleanup) {
            for (final Entry<String, ActionFrequency> entry : this.lastBreak.entrySet()) {
                if (entry.getValue().score(1f) == 0f) rem.add(entry.getKey());
            }
        } else {
            rem.addAll(this.lastBreak.keySet());
        }
        for (final String key : rem) {
            this.lastBreak.remove(key);
        }
    }

    @Override
    public void blockBreakMontitor(final Player player) {
        if (this.breakCancel > 0) {
            this.breakCancel--;
            return;
        }
        //		System.out.println("block break monitor");
        this.removeExemption(player, this.exemptBreakNormal);
    }


}
