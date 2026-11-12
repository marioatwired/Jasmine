package com.github.marioatwired.jasmine.commands.v1;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.BotConfig;
import com.github.marioatwired.jasmine.Jasmine;
import com.github.marioatwired.jasmine.commands.v1.admin.*;
import com.github.marioatwired.jasmine.commands.v1.dj.*;
import com.github.marioatwired.jasmine.commands.v1.general.SettingsCmd;
import com.github.marioatwired.jasmine.commands.v1.music.*;
import com.github.marioatwired.jasmine.commands.v1.owner.*;
import com.github.marioatwired.jasmine.commands.v2.admin.ClearchannelSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.NpButtonsSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.NpLayoutSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.PrefixSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.QueuetypeSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.SetdjSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.SettingsSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.SettcSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.SetvcSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.admin.SkipratioSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.ClearSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.ForceskipSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.ForceremoveSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.MovetrackSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.PauseSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.PlaynextSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.RepeatSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.SkiptoSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.StopSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.dj.VolumeSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.HistorySlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.NowPlayingSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.NpSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.PlaySlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.PlaylistSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.PlaylistsSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.QueueSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.RemoveSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.SearchSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.SeekSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.ShuffleSlashCmd;
import com.github.marioatwired.jasmine.commands.v2.music.SkipSlashCmd;
import com.github.marioatwired.jasmine.settings.SettingsManager;
import net.dv8tion.jda.api.OnlineStatus;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class CommandFactory {

    public static CommandClient createCommandClient(BotConfig config, SettingsManager settings, Bot bot) {
        AboutCmd aboutCommand = createAboutCommand();

        CommandClientBuilder cb = new CommandClientBuilder()
            .setPrefix(config.getPrefix())
            .setAlternativePrefix(config.getAltPrefix())
            .setOwnerId(Long.toString(config.getOwnerId()))
            .setEmojis(config.getSuccess(), config.getWarning(), config.getError())
            .setHelpWord(config.getHelp())
            .setLinkedCacheSize(200)
            .setGuildSettingsManager(settings)
            .addCommands(aboutCommand,
                    new PingCommand(),
                    new SettingsCmd(bot),
                    new NowPlayingCmd(bot),
                    new PlayCmd(bot),
                    new PlaylistsCmd(bot),
                    new QueueCmd(bot),
                    new HistoryCmd(bot),
                    new RemoveCmd(bot),
                    new SearchCmd(bot),
                    new SCSearchCmd(bot),
                    new SeekCmd(bot),
                    new ShuffleCmd(bot),
                    new SkipCmd(bot),

                    new ForceRemoveCmd(bot),
                    new ForceskipCmd(bot),
                    new MoveTrackCmd(bot),
                    new PauseCmd(bot),
                    new PlaynextCmd(bot),
                    new RepeatCmd(bot),
                    new SkiptoCmd(bot),
                    new StopCmd(bot),
                    new ClearCmd(bot),
                    new VolumeCmd(bot),

                    new PrefixCmd(bot),
                    new QueueTypeCmd(bot),
                    new NplayoutCmd(bot),
                    new NpbuttonsCmd(bot),
                    new SetdjCmd(bot),
                    new SkipratioCmd(bot),
                    new SettcCmd(bot),
                    new SetvcCmd(bot),

                    new AutoplaylistCmd(bot),
                    new DebugCmd(bot),
                    new PlaylistCmd(bot),
                    new SetavatarCmd(bot),
                    new SetgameCmd(bot),
                    new SetnameCmd(bot),
                    new SetstatusCmd(bot),
                    new ShutdownCmd(bot),
                    new ClearchannelCmd(bot)
            ).addSlashCommands(
                    // Music commands
                    new NowPlayingSlashCmd(bot),
                    new NpSlashCmd(bot),
                    new PlaySlashCmd(bot),
                    new PlaylistSlashCmd(bot),
                    new PlaylistsSlashCmd(bot),
                    new QueueSlashCmd(bot),
                    new HistorySlashCmd(bot),
                    new RemoveSlashCmd(bot),
                    new SearchSlashCmd(bot),
                    new SeekSlashCmd(bot),
                    new ShuffleSlashCmd(bot),
                    new SkipSlashCmd(bot),

                    // DJ commands
                    new ForceremoveSlashCmd(bot),
                    new ForceskipSlashCmd(bot),
                    new MovetrackSlashCmd(bot),
                    new PauseSlashCmd(bot),
                    new PlaynextSlashCmd(bot),
                    new RepeatSlashCmd(bot),
                    new SkiptoSlashCmd(bot),
                    new StopSlashCmd(bot),
                    new ClearSlashCmd(bot),
                    new VolumeSlashCmd(bot),

                    // Admin commands
                    new PrefixSlashCmd(bot),
                    new QueuetypeSlashCmd(bot),
                    new NpLayoutSlashCmd(bot),
                    new NpButtonsSlashCmd(bot),
                    new SetdjSlashCmd(bot),
                    new SettcSlashCmd(bot),
                    new SetvcSlashCmd(bot),
                    new SkipratioSlashCmd(bot),
                    new SettingsSlashCmd(bot),
                    // Owner commands
                    new ClearchannelSlashCmd(bot)
            ).setManualUpsert(true);

        if (config.useEval())
            cb.addCommand(new EvalCmd(bot));

        if (config.getStatus() != OnlineStatus.UNKNOWN)
            cb.setStatus(config.getStatus());

        if (config.getGame() == null)
            cb.useDefaultGame();
        else if (config.isGameNone())
            cb.setActivity(null);
        else
            cb.setActivity(config.getGame());

        return cb.build();
    }

    private static @NotNull AboutCmd createAboutCommand() {
        return new AboutCmd(Color.BLUE.brighter(),
                "a music bot that is [easy to host yourself!](https://github.com/marioatwired/Jasmine)",
                new String[]{"High-quality music playback", "FairQueue™ Technology", "Easy to host yourself"},
                Jasmine.RECOMMENDED_PERMS);
    }
}