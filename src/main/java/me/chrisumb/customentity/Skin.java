package me.chrisumb.customentity;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * A Minecraft skin, represented as it's value and signature.
 */
public final class Skin {

    /**
     * The data of the skin.
     */
    private final String value;
    /**
     * The Mojang required signature.
     */
    private final String signature;

    private Skin(String value, String signature) {
        this.value = value;
        this.signature = signature;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    /**
     * Saves this skin to a file.
     *
     * @param file The {@link File} to write this to.
     */
    public void save(File file) {
        try {
            Files.writeString(file.toPath(), value + "\n" + signature, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String SKIN_DATA_UUID_DOWNLOAD_URL =
            "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    private static final String PLAYER_UUID_FROM_USERNAME_URL = "https://api.mojang.com/users/profiles/minecraft/%s";

    private static UUID toRealUUID(String mojangUUID) {
        String least = mojangUUID.substring(0, 16);
        String most = mojangUUID.substring(16);
        return new UUID(Long.parseUnsignedLong(least, 16), Long.parseUnsignedLong(most, 16));
    }

    public static Skin load(File file) {
        try {
            String s = Files.readString(file.toPath());

            String[] split = s.split("\r?\n");

            if (split.length <= 0) {
                return null;
            }

            return new Skin(split[0], split[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * It's worth noting that downloading using a username is less efficient.
     *
     * @param username The username of the player to download the skin of.
     * @return The {@link Skin} of the player, or null if something went wrong.
     */
    public static Skin download(String username) {
        String url = String.format(PLAYER_UUID_FROM_USERNAME_URL, username);
        try {
            URLConnection connection = new URL(url).openConnection();
            InputStreamReader inputStream = new InputStreamReader(connection.getInputStream());
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(inputStream);

            String id = (String) jsonObject.get("id");
            return download(toRealUUID(id));

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param id The UUID of the player to download the skin of.
     * @return The {@link Skin} of the player, or null if something went wrong.
     */
    public static Skin download(UUID id) {
        String url = String.format(SKIN_DATA_UUID_DOWNLOAD_URL, id.toString().replace("-", ""));

        try {
            URLConnection connection = new URL(url).openConnection();
            InputStreamReader inputStream = new InputStreamReader(connection.getInputStream());

            JSONObject jsonObject = (JSONObject) new JSONParser().parse(inputStream);

            JSONArray properties = (JSONArray) jsonObject.get("properties");
            JSONObject property = (JSONObject) properties.get(0);

            return new Skin((String) property.get("value"), (String) property.get("signature"));

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
