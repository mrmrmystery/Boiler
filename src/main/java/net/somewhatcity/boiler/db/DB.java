package net.somewhatcity.boiler.db;

import net.somewhatcity.boiler.Boiler;
import org.bukkit.configuration.file.FileConfiguration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import java.util.HashMap;
import java.util.Map;

public class DB {

    public static SessionFactory sessionFactory;
    public static Session session;

    private static String dbType;

    private static String url;
    private static String password;
    private static String username;

    public static void init() {
        FileConfiguration config = Boiler.getPlugin().getConfig();
        dbType = config.getString("database.type", "sqlite");
        switch (dbType) {
            case "sqlite":
                String filename = config.getString("database.file", "mapdisplays.db");
                url = "jdbc:sqlite:%s".formatted(Boiler.getPlugin().getDataFolder() + "/" + filename);
                break;
            case "mysql":
                url = "jdbc:mysql://%s:%d/%s".formatted(
                        config.getString("database.host", "localhost"),
                        config.getInt("database.port", 3306),
                        config.getString("database.database", "mapdisplays")
                );
                break;
        }

        sessionFactory = create();
        session = sessionFactory.getCurrentSession();
    }

    public static Session openSession() {
        return sessionFactory.openSession();
    }

    private static SessionFactory create() {
        SessionFactory sessionFactory;

        Map<String, Object> settings = new HashMap<>();

        settings.put("hibernate.connection.url", url);
        if(dbType.equals("sqlite")) settings.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        settings.put("hibernate.current_session_context_class", "thread");
        settings.put("hibernate.show_sql", "false");
        settings.put("hibernate.format_sql", "true");
        settings.put("hibernate.hbm2ddl.auto", "update");


        try {
            ServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(settings).build();

            Metadata metadata = new MetadataSources(standardRegistry)
                    .addAnnotatedClasses(
                            SMapDisplay.class
                    )
                    .getMetadataBuilder()
                    .build();

            sessionFactory = metadata.getSessionFactoryBuilder().build();

            return sessionFactory;
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

}
