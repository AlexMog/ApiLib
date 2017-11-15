package alexmog.apilib.dao;

import java.sql.SQLException;

import com.jolbox.bonecp.BoneCPDataSource;

public abstract class DAO<T> {
	protected BoneCPDataSource mDataSource;
	
	public DAO(BoneCPDataSource dataSource) {
		mDataSource = dataSource;
	}
	
	public abstract int insert(T data) throws SQLException;
	public abstract T update(T data) throws SQLException;
}
