package com.lendingdesk.persistence.adapter;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.DeviceStatus;
import com.lendingdesk.core.model.DeviceListItem;
import com.lendingdesk.core.model.DeviceSearch;
import com.lendingdesk.core.model.PageResult;
import com.lendingdesk.core.port.DeviceRepository;
import com.lendingdesk.persistence.jpa.JpaDeviceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRepositoryAdapter implements DeviceRepository {

  private static final String FROM_DEVICES_WITH_ACTIVE_LOAN =
      " from Device d left join Loan l on l.device = d and l.returnedAt is null";

  private final JpaDeviceRepository jpa;

  @PersistenceContext private EntityManager entityManager;

  public DeviceRepositoryAdapter(JpaDeviceRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Device> findById(Long id) {
    return jpa.findById(id);
  }

  @Override
  public boolean existsByInventoryTag(String inventoryTag) {
    return jpa.existsByInventoryTag(inventoryTag);
  }

  @Override
  public Device save(Device device) {
    return jpa.save(device);
  }

  @Override
  public PageResult<DeviceListItem> search(DeviceSearch search) {
    StringBuilder where = new StringBuilder(" where 1 = 1");
    Map<String, Object> parameters = new HashMap<>();
    if (search.q() != null) {
      where.append(
          " and (lower(d.name) like :q escape '!' or lower(d.inventoryTag) like :q escape '!')");
      parameters.put("q", "%" + escapeLike(search.q().toLowerCase(Locale.ROOT)) + "%");
    }
    if (search.category() != null) {
      where.append(" and d.category = :category");
      parameters.put("category", search.category());
    }
    if (search.available() != null) {
      where.append(
          search.available()
              ? " and d.status = :availableStatus and l.id is null"
              : " and (d.status <> :availableStatus or l.id is not null)");
      parameters.put("availableStatus", DeviceStatus.AVAILABLE);
    }
    // sortField is validated against a fixed list by DeviceService, so it is safe to inline.
    String orderBy =
        " order by d." + search.sortField() + (search.ascending() ? " asc" : " desc") + ", d.id asc";

    TypedQuery<Object[]> pageQuery =
        entityManager.createQuery(
            "select d, l.employee.id" + FROM_DEVICES_WITH_ACTIVE_LOAN + where + orderBy,
            Object[].class);
    TypedQuery<Long> countQuery =
        entityManager.createQuery(
            "select count(d)" + FROM_DEVICES_WITH_ACTIVE_LOAN + where, Long.class);
    parameters.forEach(
        (name, value) -> {
          pageQuery.setParameter(name, value);
          countQuery.setParameter(name, value);
        });

    List<DeviceListItem> items =
        pageQuery
            .setFirstResult(search.page() * search.size())
            .setMaxResults(search.size())
            .getResultList()
            .stream()
            .map(row -> new DeviceListItem((Device) row[0], (Long) row[1]))
            .toList();
    long total = countQuery.getSingleResult();
    int totalPages = (int) ((total + search.size() - 1) / search.size());
    return new PageResult<>(items, search.page(), search.size(), total, totalPages);
  }

  private static String escapeLike(String text) {
    return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }
}
