import com.litovskiy.config.SchemaMigration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SchemaMigrationTest {

    @Test
    @DisplayName("Growth style constraint SQL includes all enum values")
    void growthStyleConstraintSqlIncludesAllEnumValues() {
        String sql = SchemaMigration.buildConversationSettingsGrowthStyleConstraintSql();

        Assertions.assertTrue(sql.contains("'DICK'"));
        Assertions.assertTrue(sql.contains("'EMOTIONAL_INTELLIGENCE'"));
        Assertions.assertTrue(sql.contains("'NATIONALISM'"));
        Assertions.assertTrue(sql.contains("'RUSSIAN'"));
    }
}
