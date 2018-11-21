package alexmog.apilib.dao;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class DAO {
	private DataSourceThreadLocal mDataSource;
	
	public void setDataSource(DataSourceThreadLocal dataSource) {
		mDataSource = dataSource;
	}
	
	public Connection getConnection() throws SQLException {
		return mDataSource.getConnection();
	}
}
