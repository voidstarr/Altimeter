package tv.voidstar.altimeter.data;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.misc.BaseDaoEnabled;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "ips")
public class IPData extends BaseDaoEnabled<IPData, String> {

    @DatabaseField(id = true)
    public String ip;

    @ForeignCollectionField(eager = true)
    public ForeignCollection<AccountData> accounts;

}
