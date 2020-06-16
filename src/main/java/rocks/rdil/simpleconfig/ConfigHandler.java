package rocks.rdil.simpleconfig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import lombok.Cleanup;

@SuppressWarnings("deprecation")
public final class ConfigHandler {
    private JsonObject cfg;
    private final List<Config> configObjs;
    private final File file;

    public final JsonObject getCfg() {
        return this.cfg;
    }

    public final void register(Config config) {
        this.configObjs.add(config);
        Field[] classFields = config.getClass().getDeclaredFields();
        Collection<Field> dest = new ArrayList<>();

        for (Field f : classFields) {
            if (f.isAnnotationPresent(Configuration.class)) {
                dest.add(f);
            }
        }

        for (Field it : dest) {
            if (!it.isAccessible()) {
                it.setAccessible(true);
            }

            Configuration conf = it.getAnnotation(Configuration.class);
            if (this.cfg.has(conf.alt())) {
                if (conf.alt().length() > 0 && !this.cfg.has(it.getName())) {
                    JsonElement var22 = this.cfg.get(conf.alt());
                    if (var22.getAsJsonObject().has(conf.alt())) {
                        this.cfg.add(it.getName(), this.cfg.get(conf.alt()).getAsJsonObject().get(conf.alt()));
                    }
                }
            }

            if (this.cfg.has(it.getName())) {
                try {
                    it.set(config, GsonExt.gson.fromJson(this.cfg.get(it.getName()), it.getType()));
                } catch (Exception var13) {
                    throw new IllegalStateException("Config options cannot be final!");
                }
            }
        }
    }

    private void loadConfigurationToJsonFile(Config config) {
        Field[] fields = config.getClass().getDeclaredFields();
        Collection<Field> dest = new ArrayList<>();

        for (Field theField : fields) {
            if (theField.isAnnotationPresent(Configuration.class)) {
                dest.add(theField);
            }
        }

        Field it;
        try {
            for (Iterator<Field> var4 = dest.iterator(); var4.hasNext(); this.cfg.add(it.getName(),
                    GsonExt.gson.toJsonTree(it.get(config), it.getType()))) {
                it = var4.next();
                if (!it.isAccessible()) {
                    it.setAccessible(true);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public final void save() {
        for (Config it : this.configObjs) {
            this.loadConfigurationToJsonFile(it);
        }

        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(this.file);
            String text = GsonExt.gson.toJson(this.cfg);
            fos.write(text.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public ConfigHandler(File file) {
        this.file = file;
        this.cfg = new JsonObject();
        this.configObjs = new ArrayList<>();
        try {
            if (this.file.exists()) {
                StringBuilder builder = new StringBuilder();
                @Cleanup FileReader fr = new FileReader(this.file);
                @Cleanup BufferedReader br = new BufferedReader(fr);
                String line;
                while ((line = br.readLine()) != null) {
                    builder.append(line);
                }

                this.cfg = new JsonParser().parse(builder.toString()).getAsJsonObject();
            } else {
                this.save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
