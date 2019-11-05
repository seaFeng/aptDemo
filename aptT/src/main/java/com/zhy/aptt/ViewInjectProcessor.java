package com.zhy.aptt;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zhy.lib_annotations.BindViewT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * Created by 张海洋 on 2019-02-03.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.zhy.lib_annotations.BindViewT"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ViewInjectProcessor extends AbstractProcessor {
    // 存放同一个class下所有注解
    Map<String,List<VariableInfo>> classMap = new HashMap<>();
    // 存放Class对应TypeElement
    Map<String,TypeElement> classTypeElement = new HashMap<>();

    private Filer filer;
    Elements elementUtils;
    Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        messager.printMessage(Diagnostic.Kind.NOTE,"1111111111111111111111111111111111111");
        collectInfo(roundEnvironment);
        writeToFile();
        return false;
    }

    void collectInfo(RoundEnvironment roundEnvironment) {
        classMap.clear();
        classTypeElement.clear();

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindViewT.class);
        for (Element element : elements) {
            // 获取BindView注解的值
            int viewId = element.getAnnotation(BindViewT.class).value();

            // 代表被注解的元素。
            VariableElement variableElement = (VariableElement) element;

            // 被注解元素所在的class
            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();

            String classFullName = typeElement.getQualifiedName().toString();

            // 收集Class中所有被注解的元素
            List<VariableInfo> variableInfoList = classMap.get(classFullName);
            if (variableInfoList == null) {
                variableInfoList = new ArrayList<>();
                classMap.put(classFullName,variableInfoList);
                // 保存class对应要素（名称、完整路径等）
                classTypeElement.put(classFullName,typeElement);
            }

            VariableInfo variableInfo = new VariableInfo();
            variableInfo.setVariableElement(variableElement);
            variableInfo.setViewId(viewId);
            variableInfoList.add(variableInfo);
        }
    }

    void writeToFile() {
        for (String classFullName : classMap.keySet()) {
            TypeElement typeElement = classTypeElement.get(classFullName);

            // 使用构造函数绑定数据
            MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(TypeName.get(typeElement.asType()),"activity").build());

            List<VariableInfo> variableInfoList = classMap.get(classFullName);

            for (VariableInfo variableInfo : variableInfoList) {
                VariableElement variableElement = variableInfo.getVariableElement();
                // 变量名称(比如：TextView tv)
                String variableName = variableElement.getSimpleName().toString();
                // 变量类型的完整类路径（比如：android.wight.TextView）
                String variableFullName = variableElement.asType().toString();
                // 在构造方法中增加赋值语句，例如：Activity.tv = (android.widget.TextView) activity.findViewByID(2123124)
                constructor.addStatement("activity.$L = ($L)activity.findViewById($L)",variableName,variableFullName,variableInfo.getViewId());
            }

            //构建Class
            TypeSpec typeSpec = TypeSpec.classBuilder(typeElement.getSimpleName() + "$$ViewInjector")
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(constructor.build())
                    .build();

            // 与目标Class放在同一个包下，解决Class属性的可访问性
            String packageFullName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            JavaFile javaFile = JavaFile.builder(packageFullName,typeSpec).build();

            // 生成class文件
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
