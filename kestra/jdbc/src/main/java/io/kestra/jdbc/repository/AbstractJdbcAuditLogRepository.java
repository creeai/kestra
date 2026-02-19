package io.kestra.jdbc.repository;

import io.kestra.core.models.audit.AuditLog;
import io.kestra.core.repositories.ArrayListTotal;
import io.kestra.core.repositories.AuditLogRepositoryInterface;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static io.kestra.jdbc.repository.AbstractJdbcRepository.KEY_FIELD;

public abstract class AbstractJdbcAuditLogRepository extends AbstractJdbcCrudRepository<AuditLog> implements AuditLogRepositoryInterface {

    private static final int MAX_FETCH = 10_000;

    public AbstractJdbcAuditLogRepository(io.kestra.jdbc.AbstractJdbcRepository<AuditLog> jdbcRepository) {
        super(jdbcRepository);
    }

    @Override
    protected Condition defaultFilter(String tenantId) {
        return DSL.trueCondition();
    }

    @Override
    protected Condition defaultFilter() {
        return DSL.trueCondition();
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        return create(auditLog);
    }

    @Override
    public ArrayListTotal<AuditLog> find(
        Integer page,
        Integer size,
        String sort,
        Instant from,
        Instant to,
        String actorId,
        String resourceType,
        String resourceId,
        String namespace
    ) {
        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 25;

        List<AuditLog> all = findAll(DSL.trueCondition());
        if (all.size() > MAX_FETCH) {
            all = all.stream().limit(MAX_FETCH).toList();
        }

        Stream<AuditLog> stream = all.stream();

        if (from != null) {
            stream = stream.filter(log -> log.getTimestamp() != null && !log.getTimestamp().isBefore(from));
        }
        if (to != null) {
            stream = stream.filter(log -> log.getTimestamp() != null && !log.getTimestamp().isAfter(to));
        }
        if (actorId != null && !actorId.isBlank()) {
            stream = stream.filter(log -> actorId.equals(log.getActorId()));
        }
        if (resourceType != null && !resourceType.isBlank()) {
            stream = stream.filter(log -> resourceType.equals(log.getResourceType()));
        }
        if (resourceId != null && !resourceId.isBlank()) {
            stream = stream.filter(log -> resourceId.equals(log.getResourceId()));
        }
        if (namespace != null && !namespace.isBlank()) {
            stream = stream.filter(log -> namespace.equals(log.getNamespace()) || (log.getNamespace() != null && log.getNamespace().startsWith(namespace)));
        }

        boolean desc = sort != null && sort.toLowerCase().contains(":desc");
        Comparator<AuditLog> byTimestamp = Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Instant::compareTo));
        stream = desc ? stream.sorted(byTimestamp.reversed()) : stream.sorted(byTimestamp);

        List<AuditLog> list = stream.toList();
        long total = list.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        List<AuditLog> pageList = fromIndex < list.size() ? list.subList(fromIndex, toIndex) : List.of();

        return new ArrayListTotal<>(pageList, total);
    }
}
