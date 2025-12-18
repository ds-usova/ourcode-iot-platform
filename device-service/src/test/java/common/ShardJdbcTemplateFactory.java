package common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Utility class to create JdbcTemplates for direct access to shards
 */
public class ShardJdbcTemplateFactory {

    private ShardJdbcTemplateFactory() { }

    public static JdbcTemplate createShard0Template() {
        String url = System.getProperty("SHARD_0_JDBC_URL");
        String user = System.getProperty("SHARD_0_USER");
        String password = System.getProperty("SHARD_0_PASSWORD");

        return createJdbcTemplate(url, user, password);
    }

    public static JdbcTemplate createShard1Template() {
        String url = System.getProperty("SHARD_1_JDBC_URL");
        String user = System.getProperty("SHARD_1_USER");
        String password = System.getProperty("SHARD_1_PASSWORD");

        return createJdbcTemplate(url, user, password);
    }

    private static JdbcTemplate createJdbcTemplate(String url, String username, String password) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return new JdbcTemplate(ds);
    }

}
