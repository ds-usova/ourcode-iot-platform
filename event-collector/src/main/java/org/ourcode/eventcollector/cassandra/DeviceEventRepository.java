package org.ourcode.eventcollector.cassandra;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceEventRepository extends CassandraRepository<DeviceEventEntity, String> { }
