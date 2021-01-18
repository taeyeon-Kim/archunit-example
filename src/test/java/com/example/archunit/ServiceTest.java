package com.example.archunit;

import static com.tngtech.archunit.lang.Priority.HIGH;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.thirdparty.com.google.common.collect.Sets;

// @RunWith(ArchUnitRunner.class) // Remove this line for Junit 5
@AnalyzeClasses(packages = "com.example.archunit")
public class ServiceTest {

    private final Set<String> BEAN_FIELD_MODIFIER = Set.of("PRIVATE", "FINAL");

    @DisplayName("Service annotation을 가진 클래스의 이름은 Service로 끝나야 한다.")
    @ArchTest
    void haveServicePostfixOnServiceAnnotationValue(JavaClasses javaClasses) {
        ArchRule rule = ArchRuleDefinition.classes().that()
            .areAnnotatedWith(Service.class)
            .should()
            .haveSimpleNameEndingWith("Service");

        rule.check(javaClasses);
    }

    @DisplayName("Serivce annotation을 가진 클래스는 com.example.archunit.service 패키지 아래에 있어야 한다.")
    @ArchTest
    void mustBeUnderServicePackageClassWithServiceAnnotation(JavaClasses javaClasses) {
        ArchRule rule = ArchRuleDefinition.priority(HIGH)
            .classes().that()
            .areAnnotatedWith(Service.class)
            .should()
            .resideInAPackage("com.example.archunit.service");

        rule.as("Serivce annotation을 가진 클래스는 com.example.archunit.service 패키지 아래에 있어야 한다.")
            .because("그게 Service 니까 (끄덕)")
            .check(javaClasses);
    }

    @DisplayName("Field Injecion을 사용하지 않고 Constructor Injection을 사용해야 한다.")
    @ArchTest
    void MustBeConstructorInjectionToBean(JavaClasses javaClasses) {
        ArchRule rule = ArchRuleDefinition.classes()
            .that(haveClassWithServiceAnnotation())
            .should(haveNotAFieldAnnotatedWithAutowired())
            .andShould(onlyBeConstructorInjection());

        rule.check(javaClasses);
    }

    public DescribedPredicate<JavaClass> haveClassWithServiceAnnotation() {
        return new DescribedPredicate<>("class annotated with @Service") {
            @Override
            public boolean apply(JavaClass input) {
                try {
                    input.getAnnotationOfType(Service.class);
                    return true;
                }  catch (IllegalArgumentException e) {
                    return false;
                }
            }
        };
    }

    public ArchCondition<JavaClass> haveNotAFieldAnnotatedWithAutowired() {
        return new ArchCondition<>("have not a field annotated with @Autowired") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaField field : item.getAllFields()) {
                    try {
                        field.getAnnotationOfType(Autowired.class);
                        events.add(SimpleConditionEvent.violated(field.getName(),
                            String.format("%s have not a field annotated with @Autowired", field.getName())));
                    } catch (IllegalArgumentException e) {
                        // noop
                    }
                }
            }
        };
    }

    public ArchCondition<JavaClass> onlyBeConstructorInjection() {
        return new ArchCondition<>("only be constructor injection") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                long beanFieldSize = item.getAllFields().stream()
                    .filter(isBeanField())
                    .count();
                boolean result = false;
                for (JavaConstructor constructor : item.getAllConstructors()) {
                    if (constructor.getRawParameterTypes().size() == beanFieldSize) {
                        result = true;
                    }
                }

                if (!result) {
                    events.add(
                        SimpleConditionEvent.violated(item.getName(), String.format("Bean must be constructor injection in %s", item.getName())));
                }
            }
        };
    }

    private Predicate<JavaField> isBeanField() {
        return javaField -> javaField.getModifiers().size() == BEAN_FIELD_MODIFIER.size()
            && Sets.difference(BEAN_FIELD_MODIFIER, javaField.getModifiers()
            .stream()
            .map(Enum::name)
            .collect(Collectors.toSet())).size() == 0;
    }
}