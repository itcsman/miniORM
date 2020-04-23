package cn.itcsman.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Mr.Gibson
 * @Date 2020/4/21 20:34
 * @Version 1.0.0
 * @Description :用来设置表名
 */
@Retention(RetentionPolicy.RUNTIME) //运行期间保留注解的信息
@Target(ElementType.TYPE) //设置注解用在类上
public @interface Table {


    public String name() default "" ;
}
