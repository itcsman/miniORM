package cn.itcsman.orm.core;

import cn.itcsman.orm.utils.AnnotationUtil;
import cn.itcsman.orm.utils.Dom4jUtils;
import org.dom4j.Document;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author Mr.Gibson
 * @Date 2020/4/22 19:52
 * @Version 1.0.0
 * @Description 用来解析和封装框架的核心配置文件数据
 */
public class ORMConfigure implements Serializable {
    //classpath路径
    private static String classPath;
    //核心配置文件
    private static File cfgFile;
    //<property>标签中的值
    private static Map<String, String> propConfig;
    //xml配置文件的路径
    private static Set<String> mappingSet;
    //注解配置文件的路径
    private static Set<String> entitySet;
    //解析后的映射信息
     static List<Mapper> mapperList;

    /**
     * 使用静态代码块的方式为成员变量赋值
     */
    static {
        //得到classpath的路径
        classPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        try {//给路径设置编码
            classPath = URLDecoder.decode(classPath, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //获取核心配置文件
        cfgFile = new File(classPath + "miniORM.cfg.xml");
        if (cfgFile.exists()){
            //解析配置文件中的数据
            Document document = Dom4jUtils.getXMLByFilePath(cfgFile.getPath());
            propConfig = Dom4jUtils.Elements2Map(document, "property", "name");
            mappingSet = Dom4jUtils.Elements2Set(document, "mapping", "resource");
            entitySet = Dom4jUtils.Elements2Set(document, "entity", "package");
        }else {
            //不存在该文件就赋值为null 必须要赋值才行不能不管，因为成员变量没有初始化
            cfgFile = null;
            System.out.println("核心配置文件不存在！");
        }

    }

    //从propConfig获取信息链接数据库
    private Connection getConnection(){
        /**
         *     <property name="connection.url">jdbc:mysql://localhost:3306/test</property>
         *     <property name="connection.driverClass">com.mysql.jdbc.Driver</property>
         *     <property name="connection.username">root</property>
         *     <property name="connection.password">123</property>
         */
        String url = propConfig.get("connection.url");
        String driverClass = propConfig.get("connection.driverClass");
        String username = propConfig.get("connection.username");
        String password = propConfig.get("connection.password");
        try {
            Class.forName(driverClass);
            Connection connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(true);//设置自动提交事务
            return connection;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取映射配置文件的参数
     * @throws ClassNotFoundException
     */
    private void getMapping() throws ClassNotFoundException {
//        解析xml映射配置文件
        mapperList = new ArrayList<>();
        for (String xmlPath : mappingSet) {
            Document document = Dom4jUtils.getXMLByFilePath(classPath + xmlPath);
            String className = Dom4jUtils.getPropValue(document, "class", "name");
            String tableName = Dom4jUtils.getPropValue(document, "class", "table");
            Map<String, String> idMapper = Dom4jUtils.ElementsID2Map(document);
            Map<String, String> propMapper = Dom4jUtils.Elements2Map(document);
            Mapper mapper = new Mapper();
            mapper.setClassName(className);
            mapper.setIdMapper(idMapper);
            mapper.setPropMapper(propMapper);
            mapper.setTableName(tableName);
            mapperList.add(mapper);
        }

//        解析实体类注解配置
        for (String packagePath : entitySet) {

            Set<String> entityNames = AnnotationUtil.getClassNameByPackage(packagePath);
            for (String entityName : entityNames) {
                Class clz = Class.forName(entityName);
                String className = AnnotationUtil.getClassName(clz);
                Map<String, String> idMapper = AnnotationUtil.getIdMapper(clz);
                String tableName = AnnotationUtil.getTableName(clz);
                Map<String, String> propMapping = AnnotationUtil.getPropMapping(clz);
                Mapper mapper = new Mapper();
                mapper.setTableName(tableName);
                mapper.setPropMapper(propMapping);
                mapper.setIdMapper(idMapper);
                mapper.setClassName(className);
                mapperList.add(mapper);
            }
        }
    }

    public ORMSession buildORMSession() throws ClassNotFoundException {
        //连接数据库
        Connection connection = getConnection();
        //获取映射数据
        getMapping();
        return new ORMSession(connection);
    }
}
