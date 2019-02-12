package com.nexus.ytd;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTPlayer {

    public interface DecryptInterface {
        String decrypt_s(String s);
    }

    private DecryptInterface decryptInterface;

    YTPlayer(String playerUrl) throws UnknownError, IOException, ScriptException {
        Matcher m;

        m = Pattern.compile("player.*-(?<id>[^/\\\\]+)[/\\\\]+.*\\.js$").matcher(playerUrl);
        String id;
        if (m.find()) id = m.group("id");
        else throw new UnknownError("Unknown player");


		//noinspection ResultOfMethodCallIgnored
		new File("cache").mkdir();
        File playerFile = new File("cache/player-" + id + ".js");

        if (!playerFile.exists()) {
            // Must download new player
            String js_code = Utils.downloadURLToString(playerUrl);

            // Extract required JavaScript code
            m = Pattern.compile("(?<=[\\s;])c\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?(?<method>[a-zA-Z0-9$]+)\\(").matcher(js_code);
            if (!m.find()) throw new UnknownError("Could not find method name");
            String methodName = m.group("method");

            m = Pattern.compile("(?<=[\\s;])" + methodName + "\\s*=\\s*function\\s*\\([^)]+\\)\\s*\\{").matcher(js_code);
            if (!m.find()) throw new UnknownError("Could not find definition for " + methodName);
            String code1 = Utils.extractJSObject(js_code, m.start());

            m = Pattern.compile("function\\s*\\(\\s*(?<arg>[0-9-A-Za-z]+)\\s*\\)\\s*\\{[\\S\\s]*?(?<obj>[0-9A-Za-z]+)\\.[0-9A-Za-z]+\\(\\s*\\k<arg>\\s*,").matcher(code1);
            if (!m.find()) throw new UnknownError("Could not find object in method " + methodName);
            String obj = m.group("obj");

            m = Pattern.compile("(?<=[\\s;])" + obj + "\\s*=\\s*\\{").matcher(js_code);
            if (!m.find()) throw new UnknownError("Could not find definition for " + obj);
            String code2 = Utils.extractJSObject(js_code, m.start());

            // Compile new script
            js_code = "var " + code2 + "var " + code1 + "var decrypt_s=" + methodName + ";";
            Files.write(playerFile.toPath(), js_code.getBytes());
        }

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        engine.eval(new FileReader(playerFile));

        Invocable inv = (Invocable) engine;
        decryptInterface = inv.getInterface(DecryptInterface.class);
        if (decryptInterface == null)
            throw new UnknownError("Could not extract method");
    }

    public void decryptFormat(YTFormat f) {
        if (!f.isEncrypted())
            return;
        f.decrypt(decryptInterface.decrypt_s(f.getEncryptedSignature()));
    }

}
