package ftsdocs;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ServerBeanCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Object source = metadata.getAnnotations().get(Conditional.class).getSource();
        String serverClassName = source.toString();
        return Configuration.serverClassName.equals(serverClassName);
    }
}