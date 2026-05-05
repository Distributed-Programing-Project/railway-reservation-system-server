package vn.edu.iuh.fit.server.util.id;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class CustomerIdGenerator implements IdentifierGenerator {

  private static final String SEQ_TABLE = "hibernate_custom_id_sequences";
  private static volatile boolean sequenceTableEnsured = false;
  private static final Object sequenceTableLock = new Object();

  @Override
  public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
    String prefix = "KH";
    String seqName = "CUSTOMER";

    long next = nextSequenceValue(session, seqName, () -> initializeFromExisting(session, prefix));
    return prefix + String.format("%09d", next);
  }

  private long nextSequenceValue(
      SharedSessionContractImplementor session,
      String seqName,
      InitialValueSupplier initialValueSupplier) {
    Connection connection = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
    ensureSequenceTable(connection);

    try {
      Long current = selectForUpdate(connection, seqName);
      if (current == null) {
        long initial = initialValueSupplier.get();
        insertSequenceRow(connection, seqName, initial + 1);
        return initial;
      }

      updateSequenceRow(connection, seqName, current + 1);
      return current;
    } catch (SQLException e) {
      throw new HibernateException("Failed to generate Customer id", e);
    }
  }

  private long initializeFromExisting(SharedSessionContractImplementor session, String prefix) {
    Connection connection = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
    String like = prefix + "%";
    String sql = "select max(customer_id) from customers where customer_id like ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, like);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return 1L;
        }
        String maxId = rs.getString(1);
        if (maxId == null || maxId.length() != 11) {
          return 1L;
        }
        long numeric = Long.parseLong(maxId.substring(2));
        return numeric + 1;
      }
    } catch (SQLException | NumberFormatException e) {
      throw new HibernateException("Failed to initialize Customer sequence from existing data", e);
    }
  }

  private static void ensureSequenceTable(Connection connection) {
    if (sequenceTableEnsured) {
      return;
    }
    synchronized (sequenceTableLock) {
      if (sequenceTableEnsured) {
        return;
      }
      String ddl =
          "create table if not exists " + SEQ_TABLE + " ("
              + "seq_name varchar(64) not null primary key,"
              + "next_val bigint not null"
              + ")";
      try (Statement st = connection.createStatement()) {
        st.execute(ddl);
        sequenceTableEnsured = true;
      } catch (SQLException e) {
        throw new HibernateException("Failed ensuring sequence table " + SEQ_TABLE, e);
      }
    }
  }

  private static Long selectForUpdate(Connection connection, String seqName) throws SQLException {
    String sql = "select next_val from " + SEQ_TABLE + " where seq_name = ? for update";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, seqName);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return rs.getLong(1);
      }
    }
  }

  private static void insertSequenceRow(Connection connection, String seqName, long nextVal) throws SQLException {
    String sql = "insert into " + SEQ_TABLE + " (seq_name, next_val) values (?, ?)";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, seqName);
      ps.setLong(2, nextVal);
      ps.executeUpdate();
    }
  }

  private static void updateSequenceRow(Connection connection, String seqName, long nextVal) throws SQLException {
    String sql = "update " + SEQ_TABLE + " set next_val = ? where seq_name = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setLong(1, nextVal);
      ps.setString(2, seqName);
      ps.executeUpdate();
    }
  }

  @FunctionalInterface
  private interface InitialValueSupplier extends Serializable {
    long get();
  }
}

