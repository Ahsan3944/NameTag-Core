package com.ultraop.nametag.paper;

import com.ultraop.nametag.api.CommandContext;
import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.NameTagCommandHandler;
import com.ultraop.nametag.api.PermissionService;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultConfigurationService;
import com.ultraop.nametag.common.FileTagAuditLogger;
import com.ultraop.nametag.common.DefaultMessageService;
import com.ultraop.nametag.common.DefaultNameTagCommandHandler;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.YamlPlayerAssignmentRepository;
import com.ultraop.nametag.common.YamlTagRepository;
import com.ultraop.nametag.paper.v1_21_11.Paper2111Adapter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class NameTagPaperPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private TagService tagService;
    private DefaultConfigurationService configurationService;
    private NameTagCommandHandler commandHandler;
    private Paper2111Adapter adapter;
    private FileTagAuditLogger auditLogger;

    @Override
    public void onEnable() {
        java.nio.file.Path dataDirectory = getDataFolder().toPath();
        configurationService = new DefaultConfigurationService(dataDirectory.resolve("configuration.yml"));
        PermissionService permissions = new PaperPermissionService();
        tagService = new DefaultTagService(
                new YamlTagRepository(dataDirectory.resolve("tags.yml")),
                new YamlPlayerAssignmentRepository(dataDirectory.resolve("assignments.yml")),
                permissions
        );
        auditLogger = FileTagAuditLogger.register(tagService.events(), dataDirectory.resolve("audit.log"));
        commandHandler = new DefaultNameTagCommandHandler(
                tagService,
                new PaperPlayerResolver(),
                new DefaultMessageService(),
                configurationService,
                configurationService,
                dataDirectory
        );
        adapter = new Paper2111Adapter(this, tagService, configurationService);
        adapter.start();

        if (getCommand("nametag") != null) {
            getCommand("nametag").setExecutor(this);
            getCommand("nametag").setTabCompleter(this);
        }

        getLogger().info("NameTag-Core enabled.");
    }

    @Override
    public void onDisable() {
        if (adapter != null) {
            adapter.stop();
        }
        if (auditLogger != null) {
            auditLogger.close();
        }
        getLogger().info("NameTag-Core disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        commandHandler.execute(new CommandContext(new PaperCommandSource(sender), args));
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        return List.copyOf(commandHandler.suggest(
                new CommandContext(new PaperCommandSource(sender), args)
        ));
    }
}
