package org.example.nursingtrainingbackend.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
@Getter
public class CacheEvictionEvent extends ApplicationEvent {

    public enum Scope {
        DASHBOARD,
        CATEGORY_TREE,
        CATEGORY_OVERVIEW,
        STUDENT_DEPT_DISTRIBUTION,
        TAG_OVERVIEW,
        TAG_STATISTICS,
        ALL
    }

    private final Scope scope;
    private final boolean doubleDelete;
    private final long delayMillis;

    public CacheEvictionEvent(Object source, Scope scope) {
        this(source, scope, true, 500);
    }

    public CacheEvictionEvent(Object source, Scope scope, boolean doubleDelete, long delayMillis) {
        super(source);
        this.scope = scope;
        this.doubleDelete = doubleDelete;
        this.delayMillis = delayMillis;
    }
}

