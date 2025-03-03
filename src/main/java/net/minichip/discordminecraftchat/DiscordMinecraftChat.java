package net.minichip.discordminecraftchat;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class DiscordMinecraftChat extends JavaPlugin implements Listener {
    private String webhookUrl;

    @Override
    public void onEnable() {
        // 플러그인 데이터 폴더(plugins/DiscordMinecraftChat/)가 없으면 생성
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // config.yml 파일 객체 생성
        File configFile = new File(getDataFolder(), "config.yml");
        // config.yml 파일이 없으면 기본 파일 생성
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                String defaultConfig = "# DiscordMinecraftChat config file\n"
                                     + "discord_webhook: \"YOUR_WEBHOOK_URL_HERE\"\n";
                Files.write(configFile.toPath(), defaultConfig.getBytes(StandardCharsets.UTF_8));
                getLogger().info("Default config.yml created at " + configFile.getAbsolutePath());
            } catch (IOException e) {
                getLogger().severe("Could not create config.yml: " + e.getMessage());
            }
        }

        // 설정 파일 로드
        reloadConfig();
        FileConfiguration config = getConfig();
        webhookUrl = config.getString("discord_webhook", "");

        if (webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            getLogger().severe("Discord Webhook URL is not configured in config.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("DiscordMinecraftChat plugin enabled.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getPlayer().getName() + ": " + event.getMessage();
        sendDiscordMessage(message);
    }

    private void sendDiscordMessage(String message) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Discord Webhook 메시지는 JSON 형태로 전송 (예: {"content": "메시지 내용"})
            String jsonPayload = "{\"content\": \"" + message.replace("\"", "\\\"") + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT && responseCode != HttpURLConnection.HTTP_OK) {
                getLogger().warning("Discord Webhook failed: HTTP " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Exception sending Discord Webhook: " + e.getMessage());
        }
    }
}