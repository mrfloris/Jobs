package com.gamingmesh.jobs.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.actions.ItemActionInfo;
import com.gamingmesh.jobs.container.ActionType;
import com.gamingmesh.jobs.container.PlayerCamp;

public final class JobsPayment14Listener implements Listener {

    // BlockCookEvent does not have "cooking owner"
    private final Map<UUID, List<PlayerCamp>> campPlayers = new HashMap<>();

    @EventHandler(priority = EventPriority.LOW)
    public void onCook(BlockCookEvent event) {
	if (event.isCancelled() || !(event.getBlock().getType() != Material.CAMPFIRE) || campPlayers.isEmpty())
	    return;

	if (!Jobs.getGCManager().canPerformActionInWorld(event.getBlock().getWorld()))
	    return;

	for (Map.Entry<UUID, List<PlayerCamp>> map : campPlayers.entrySet()) {
	    List<PlayerCamp> camps = map.getValue();

	    if (camps.isEmpty()) {
		campPlayers.remove(map.getKey());
		continue;
	    }

	    for (PlayerCamp camp : new ArrayList<>(camps)) {
		if (camp.getBlock().getLocation().equals(event.getBlock().getLocation())) {
		    if (camp.getItem().equals(event.getSource())) {
			camps.remove(camp);

			if (camps.isEmpty()) {
			    campPlayers.remove(map.getKey());
			} else {
			    campPlayers.replace(map.getKey(), camps);
			}
		    }

		    Jobs.action(Jobs.getPlayerManager().getJobsPlayer(map.getKey()), new ItemActionInfo(event.getSource(), ActionType.BAKE));
		    break;
		}
	    }
	}
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
	if (event.getBlock().getType() != Material.CAMPFIRE || campPlayers.isEmpty())
	    return;

	List<PlayerCamp> camps = campPlayers.get(event.getPlayer().getUniqueId());

	if (camps != null) {
	    if (camps.isEmpty()) {
		campPlayers.remove(event.getPlayer().getUniqueId());
		return;
	    }

	    for (PlayerCamp camp : new ArrayList<>(camps)) {
		if (camp.getBlock().getLocation().equals(event.getBlock().getLocation())) {
		    camps.remove(camp);

		    if (camps.isEmpty()) {
			campPlayers.remove(event.getPlayer().getUniqueId());
		    } else {
			campPlayers.replace(event.getPlayer().getUniqueId(), camps);
		    }

		    break;
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCampPlace(PlayerInteractEvent ev) {
	org.bukkit.block.Block click = ev.getClickedBlock();

	if (click == null || click.getType() != Material.CAMPFIRE || !ev.hasItem())
	    return;

	if (!Jobs.getGCManager().canPerformActionInWorld(click.getWorld()) || !JobsPaymentListener.payIfCreative(ev.getPlayer()))
	    return;

	List<PlayerCamp> camps = campPlayers.getOrDefault(ev.getPlayer().getUniqueId(), new ArrayList<>());
	camps.add(new PlayerCamp(ev.getItem(), click));

	campPlayers.put(ev.getPlayer().getUniqueId(), camps);
    }
}
