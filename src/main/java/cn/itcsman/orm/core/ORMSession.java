package cn.itcsman.orm.core;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author Mr.Gibson
 * @Date 2020/4/22 21:25
 * @Version 1.0.0
 * @Description
 */
public class ORMSession {

    private Connection connection;

    public ORMSession(Connection connection) {
        this.connection = connection;
    }

    //保存数据
    public void save(Object entity) throws IllegalAccessException, SQLException {
        //从ORMConfig对象中获取映射属性
        List<Mapper> mappers =  ORMConfigure.mapperList;
//        遍历集合获取相应的mapper对象
        String insertSql1 = "";
        String insertSql2 = "";
        String insertSql = "";
        for (Mapper mapper : mappers) {
            if (mapper.getClassName().equals(entity.getClass().getName())){
                insertSql1 = "insert into " + mapper.getTableName() + "  ( ";
                insertSql2 = " ) values( ";
                //通过反射获取当前实体类的所有属性
                Field[] fileds = entity.getClass().getDeclaredFields();
                for (Field field: fileds) {
                    field.setAccessible(true);//暴力反射
                    String columnName = mapper.getPropMapper().get(field.getName());
                    String columnValue = field.get(entity).toString();
                    //拼接sql
                    insertSql1 += columnName + ",";
                    insertSql2 +=  "'" + columnValue + "',";
                }
                //拼接sql
                insertSql = insertSql1.substring(0,insertSql1.length() - 1) + insertSql2.substring(0,insertSql2.length() - 1) + " )";

                break;
            }
        }
        // 把 sql 语句打印到控制台
        System.out.println("MiniORM-save: " + insertSql);
        PreparedStatement preparedStatement = connection.prepareStatement(insertSql);
        preparedStatement.executeUpdate();
        //关闭stamt
        preparedStatement.close();
    }

    /**
     * 根据主键删除 delete from tablename where 主键 = ?
     * @param entity
     */
    public void delete(Object entity) throws NoSuchFieldException, IllegalAccessException, SQLException {
        //从ORMConfig对象中获取映射属性
        List<Mapper> mappers =  ORMConfigure.mapperList;
        String delSql = "delete from  ";
        for (Mapper mapper : mappers) {
            if (mapper.getClassName().equals(entity.getClass().getName())){
                String tableName = mapper.getTableName();
                delSql += tableName  + "  where  ";
                //获取主键的属性名
                Object idProp = mapper.getIdMapper().keySet().toArray()[0];
                //获取主键的字段名
                Object idColumn = mapper.getIdMapper().values().toArray()[0];
                //获取主键的值
                Field field = entity.getClass().getDeclaredField(idProp.toString());
                field.setAccessible(true);//暴力反射
                String idVal = field.get(entity).toString();
                //拼接sql
                delSql += idColumn.toString() + " = " + idVal;
                break;
            }
        }
        // 把 sql 语句打印到控制台
        System.out.println("MiniORM-delete: " + delSql);
        PreparedStatement preparedStatement = connection.prepareStatement(delSql);
        preparedStatement.executeUpdate();
        //关闭stamt
        preparedStatement.close();

    }

    /**
     * 根据id查询一个 select * from tableName where 主键 = ？
     * @param clz
     * @param id
     * @return
     */
    public Object findOne(Class clz, Object id) throws SQLException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        List<Mapper> mappers =  ORMConfigure.mapperList;
        String querySql = "select *  from  ";
        for (Mapper mapper : mappers) {
            if (mapper.getClassName().equals(clz.getName())){
                String tableName = mapper.getTableName();
                //获取主键的字段名
                Object idColumn = mapper.getIdMapper().values().toArray()[0];
                querySql += tableName + " where " + idColumn.toString() + " = " + id;
                break;
            }
        }
            // 把 sql 语句打印到控制台
        System.out.println("MiniORM-query: " + querySql);
        PreparedStatement preparedStatement = connection.prepareStatement(querySql);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()){//封装查询结果对象
            Object instance = clz.newInstance();
            for (Mapper mapper : mappers) {
                if (mapper.getClassName().equals(clz.getName())){
                    Map<String, String> propMapper = mapper.getPropMapper();
                    Set<String> keySet = propMapper.keySet();
                    for (String prop : keySet) {//prop是属性名
                        String  column = propMapper.get(prop); //column是和属性名对应的字段名
                        Field field = clz.getDeclaredField(prop);
                        field.setAccessible(true);//暴力反射
                        field.set(instance,resultSet.getObject(column));
                    }
                break;
                }
            }

            resultSet.close();
            preparedStatement.close();

            return instance;
        }else {
            return null;
        }
        //关闭stamt


    }

    public void close() {
        if (connection != null){
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
