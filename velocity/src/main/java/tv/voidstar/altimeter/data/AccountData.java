package tv.voidstar.altimeter.data;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.BaseDaoEnabled;
import com.j256.ormlite.table.DatabaseTable;

import java.time.Instant;
import java.util.UUID;

@DatabaseTable(tableName = "accounts")
public class AccountData extends BaseDaoEnabled<AccountData, Integer> {

    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(foreign = true)
    public IPData ip;

    @DatabaseField
    public UUID uuid;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    public Instant ttl;

}
