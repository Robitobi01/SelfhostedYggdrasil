package net.dugged.selfhostedyggdrasil;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.FieldWrapper;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ConfigCleanup {
    private ConfigCleanup() {
    }

    public static void prune(final String modid) {
        final Set<Class<?>> configClasses = new LinkedHashSet<>();
        for (final Class<?> configClass : ConfigManager.getModConfigClasses(modid)) {
            final Config config = configClass.getAnnotation(Config.class);
            if (config != null && (config.name().isEmpty() || config.name().equals(modid))) {
                configClasses.add(configClass);
            }
        }
        if (configClasses.isEmpty()) {
            return;
        }
        final Configuration configuration = cachedConfiguration(modid, modid);
        if (configuration != null && prune(configuration, configClasses)) {
            configuration.save();
        }
    }

    public static boolean prune(final Configuration configuration, final Class<?> configClass, final Class<?>... configClasses) {
        final Collection<Class<?>> classes = new ArrayList<>(configClasses.length + 1);
        classes.add(configClass);
        classes.addAll(Arrays.asList(configClasses));
        return prune(configuration, classes);
    }

    private static boolean prune(final Configuration configuration, final Collection<Class<?>> configClasses) {
        final Schema schema = new Schema();
        for (final Class<?> configClass : configClasses) {
            final Config config = configClass.getAnnotation(Config.class);
            if (config == null) {
                throw new IllegalArgumentException(configClass + " is not annotated with @Config");
            }
            schema.collect(configClass, config.category().toLowerCase(Locale.ENGLISH), true);
        }
        return schema.prune(configuration);
    }

    public static boolean pruneTracked(final TrackedConfiguration configuration) {
        return configuration.pruneTracked();
    }

    private static Configuration cachedConfiguration(final String modid, final String name) {
        try {
            final Method method = ConfigManager.class.getDeclaredMethod("getConfiguration", String.class, String.class);
            method.setAccessible(true);
            return (Configuration) method.invoke(null, modid, name);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access Forge's cached configuration", e);
        }
    }

    private static String key(final String category, final String property) {
        return category + '\0' + property;
    }

    private static final class Schema {
        private final Set<String> categories = new HashSet<>();
        private final Set<String> properties = new HashSet<>();
        private final Set<String> dynamic = new HashSet<>();

        private void collect(final Class<?> type, final String category, final boolean root) {
            addCategory(category);
            for (final Field field : type.getDeclaredFields()) {
                if (!Modifier.isPublic(field.getModifiers())) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers()) != root) {
                    continue;
                }
                if (field.isAnnotationPresent(Config.Ignore.class)) {
                    continue;
                }
                final String name = field.isAnnotationPresent(Config.Name.class)
                        ? field.getAnnotation(Config.Name.class).value()
                        : field.getName();
                if (FieldWrapper.hasWrapperFor(field)) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        final String mapCategory = qualify(category, name.toLowerCase(Locale.ENGLISH));
                        addCategory(mapCategory);
                        dynamic.add(mapCategory);
                    } else {
                        track(category, name);
                    }
                } else if (field.getType().getSuperclass() != null && field.getType().getSuperclass().equals(Object.class)) {
                    collect(field.getType(), qualify(category, name.toLowerCase(Locale.ENGLISH)), false);
                }
            }
        }

        private void addCategory(final String category) {
            if (category.isEmpty()) {
                return;
            }
            String path = category;
            while (!path.isEmpty()) {
                categories.add(path);
                final int dot = path.lastIndexOf(Configuration.CATEGORY_SPLITTER);
                if (dot < 0) {
                    break;
                }
                path = path.substring(0, dot);
            }
        }

        private void track(final String category, final String property) {
            final String normalized = category == null ? "" : category.toLowerCase(Locale.ENGLISH);
            addCategory(normalized);
            if (!normalized.isEmpty()) {
                properties.add(key(normalized, property));
            }
        }

        private String qualify(final String category, final String name) {
            return category.isEmpty() ? name : category + Configuration.CATEGORY_SPLITTER + name;
        }

        private boolean isDynamic(final String category) {
            String path = category;
            while (!path.isEmpty()) {
                if (dynamic.contains(path)) {
                    return true;
                }
                final int dot = path.lastIndexOf(Configuration.CATEGORY_SPLITTER);
                if (dot < 0) {
                    break;
                }
                path = path.substring(0, dot);
            }
            return false;
        }

        private boolean prune(final Configuration configuration) {
            boolean changed = false;
            for (final String categoryName : new ArrayList<>(configuration.getCategoryNames())) {
                if (!configuration.hasCategory(categoryName)) {
                    continue;
                }
                if (!categories.contains(categoryName) && !isDynamic(categoryName)) {
                    configuration.removeCategory(configuration.getCategory(categoryName));
                    changed = true;
                }
            }
            for (final String categoryName : categories) {
                if (!configuration.hasCategory(categoryName) || isDynamic(categoryName)) {
                    continue;
                }
                final ConfigCategory category = configuration.getCategory(categoryName);
                for (final String property : new ArrayList<>(category.keySet())) {
                    if (!properties.contains(key(categoryName, property))) {
                        category.remove(property);
                        changed = true;
                    }
                }
            }
            return changed;
        }

        private void clear() {
            categories.clear();
            properties.clear();
            dynamic.clear();
        }
    }

    public static final class TrackedConfiguration extends Configuration {
        private final Schema schema = new Schema();

        public TrackedConfiguration(final File file, final String version) {
            super(file, version);
        }

        @Override
        public Property get(final String category, final String key, final String defaultValue, final String comment, final Property.Type type) {
            schema.track(category, key);
            return super.get(category, key, defaultValue, comment, type);
        }

        @Override
        public Property get(final String category, final String key, final String[] defaultValues, final String comment, final Property.Type type) {
            schema.track(category, key);
            return super.get(category, key, defaultValues, comment, type);
        }

        private boolean pruneTracked() {
            final boolean changed = schema.prune(this);
            schema.clear();
            return changed;
        }
    }
}
