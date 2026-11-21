package com.cosync.booking.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cosync.booking.dto.zone.CreateZoneRequest;
import com.cosync.booking.dto.zone.ZoneResponse;
import com.cosync.booking.entity.Zone;
import com.cosync.booking.repository.ZoneRepository;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock ZoneRepository repo;

    ZoneService service;

    @BeforeEach
    void setUp() {
        service = new ZoneService(repo);
    }

    @Test
    @DisplayName("TC-ZON-01 list → แปลงทุกแถว")
    void list() {
        when(repo.findAll()).thenReturn(List.of(new Zone("A"), new Zone("B")));

        assertThat(service.listZones()).extracting(ZoneResponse::name).containsExactly("A", "B");
    }

    @Test
    @DisplayName("TC-ZON-02 create → ตัดเว้นวรรคหัวท้ายก่อนบันทึก (\" D \" กับ \"D\" ต้องชนกันที่ uk_zone_name)")
    void create_trims() {
        when(repo.save(any(Zone.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoneResponse r = service.createZone(new CreateZoneRequest("  D  "));

        ArgumentCaptor<Zone> saved = ArgumentCaptor.forClass(Zone.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("D");
        assertThat(r.name()).isEqualTo("D");
    }
}
