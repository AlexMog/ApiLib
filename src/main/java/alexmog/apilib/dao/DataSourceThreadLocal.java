package alexmog.apilib.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import com.zaxxer.hikari.HikariDataSource;

public class DataSourceThreadLocal {
	private final HikariDataSource mDataSource;
	private final ThreadLocal<Connection> mThreadLocal;
	
	public DataSourceThreadLocal(HikariDataSource dataSource) {
		mDataSource = dataSource;
		mThreadLocal = new ThreadLocal<>();
		ThreadLocal.withInitial(new Supplier<Connection>() {

			@Override
			public Connection get() {
				return null;
			}
		});
	}
	
	public void releaseConnection() throws SQLException {
		Connection conn = mThreadLocal.get();
		if (conn == null) return;
		mThreadLocal.remove();
		conn.close();
	}
	
	public Connection prepareDataSource() throws SQLException {
		Connection conn = mThreadLocal.get();
		if (conn != null) return conn;
		conn = mDataSource.getConnection();
		mThreadLocal.set(conn);
		return conn;
	}
	
	public Connection getConnection() throws SQLException {
		Connection conn = mThreadLocal.get();
		if (conn == null) conn = prepareDataSource();
		return conn;
	}

	public void close() {
		mDataSource.close();
	}
}
