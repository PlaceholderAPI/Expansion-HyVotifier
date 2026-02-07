package at.helpch.papi.expansion.hyvotifier;

import at.helpch.placeholderapi.PlaceholderAPIPlugin;
import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import at.helpch.placeholderapi.configuration.BooleanValue;

import javax.annotation.Nullable;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import me.hytaleserverlist.mods.hytalevotifier.HytaleVotifier;
import me.hytaleserverlist.mods.hytalevotifier.OfflineVoteManager;
import me.hytaleserverlist.mods.hytalevotifier.VoteMilestoneManager;
import me.hytaleserverlist.mods.hytalevotifier.VoteLeaderboardManager;
import me.hytaleserverlist.mods.hytalevotifier.config.VoteMilestoneConfig;

public class HyVotifierExpansion extends PlaceholderExpansion {
    @Override
    public String getIdentifier() {
        return "hyvotifier";
    }

    @Override
    public String getAuthor() {
        return "Helpchat";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String getRequiredPlugin() {
        return "HSL:Votifier";
    }

    @Override
    public String onPlaceholderRequest(PlayerRef player, String identifier) {
        if (player == null)
            return "";

        OfflineVoteManager offlineVoteManager = OfflineVoteManager.getInstance();
        VoteMilestoneManager voteMilestoneManager = VoteMilestoneManager.getInstance();
        VoteLeaderboardManager voteLeaderboardManager = VoteLeaderboardManager.getInstance();
        HytaleVotifier hytaleVotifier = HytaleVotifier.getInstance();

        switch (identifier) {
            case "offline_votes":
                return String.valueOf(offlineVoteManager.getOfflineVoteCount(player.getUsername()));
            case "has_offline_votes":
                return bool(offlineVoteManager.getOfflineVoteCount(player.getUsername()) > 0);
            case "offline_votes_enabled":
                return bool(hytaleVotifier.getOfflineVotesConfig().get().isEnabled());
            case "offline_votes_maxclaims":
                return String.valueOf(hytaleVotifier.getOfflineVotesConfig().get().getMaxClaimAmount());
            case "offline_votes_maxlifetimehrs":
                return String.valueOf(hytaleVotifier.getOfflineVotesConfig().get().getMaxLifetimeHours());
            case "milestones_enabled":
                return bool(hytaleVotifier.getVoteMilestoneConfig().get().isEnabled());
            case "milestone_votecount":
                return String.valueOf(voteMilestoneManager.getVoteCount(player.getUsername()));
            case "next_milestone":
                int voteCount = voteMilestoneManager.getVoteCount(player.getUsername());
                List<VoteMilestoneConfig.VoteMilestone> milestones = hytaleVotifier.getVoteMilestoneConfig().get()
                        .getMilestones();
                if (milestones == null || milestones.isEmpty())
                    return "0";

                int nextRequirement = Integer.MAX_VALUE;
                for (VoteMilestoneConfig.VoteMilestone milestone : milestones) {
                    int requirement = milestone.getVoteRequirement();
                    if (requirement > voteCount && requirement < nextRequirement)
                        nextRequirement = requirement;
                }

                return nextRequirement == Integer.MAX_VALUE ? "0" : String.valueOf(nextRequirement);
            case "leaderboard_votes":
                return String.valueOf(voteLeaderboardManager.getVoteCount(player.getUsername()));
            case "leaderboard_position":
                return getLeaderboardPosition(voteLeaderboardManager, player.getUsername());
        }

        if (identifier.startsWith("leaderboard_top_") && (identifier.endsWith("_name") || identifier.endsWith("_votes"))) {
            int prefixLength = "leaderboard_top_".length();
            String middle = identifier.substring(prefixLength,
                    identifier.length() - (identifier.endsWith("_name") ? 5 : 6));
            try {
                int index = Integer.parseInt(middle) - 1;
                if (identifier.endsWith("_name")) {
                    return getTopVoterName(voteLeaderboardManager, index);
                } else if (identifier.endsWith("_votes")) {
                    return getTopVoterVotes(voteLeaderboardManager, index);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;

    }

    public String bool(boolean b) {
        return b ? PlaceholderAPIPlugin.instance().configManager().config().booleanValue().trueValue()
                : PlaceholderAPIPlugin.instance().configManager().config().booleanValue().falseValue();
    }

    private String getTopVoterName(VoteLeaderboardManager voteLeaderboardManager, int index) {
        VoteLeaderboardManager.LeaderboardEntry entry = getTopVoterEntry(voteLeaderboardManager, index);
        return entry == null ? "" : entry.getUsername();
    }

    private String getTopVoterVotes(VoteLeaderboardManager voteLeaderboardManager, int index) {
        VoteLeaderboardManager.LeaderboardEntry entry = getTopVoterEntry(voteLeaderboardManager, index);
        return entry == null ? "0" : String.valueOf(entry.getVoteCount());
    }

    private VoteLeaderboardManager.LeaderboardEntry getTopVoterEntry(VoteLeaderboardManager voteLeaderboardManager,
            int index) {
        if (index < 0)
            return null;

        List<VoteLeaderboardManager.LeaderboardEntry> topVoters = voteLeaderboardManager.getTopVoters(index + 1);
        if (topVoters == null || topVoters.size() <= index)
            return null;

        return topVoters.get(index);
    }

    private String getLeaderboardPosition(VoteLeaderboardManager voteLeaderboardManager, String username) {
        if (username == null || username.isEmpty())
            return "-1";

        List<VoteLeaderboardManager.LeaderboardEntry> topVoters = voteLeaderboardManager.getTopVoters(Integer.MAX_VALUE);
        if (topVoters == null || topVoters.isEmpty())
            return "-1";

        String key = username.toLowerCase();
        for (int i = 0; i < topVoters.size(); i++) {
            VoteLeaderboardManager.LeaderboardEntry entry = topVoters.get(i);
            if (entry != null && entry.getUsername() != null
                    && entry.getUsername().toLowerCase().equals(key))
                return String.valueOf(i + 1);
        }

        return "0";
    }
}