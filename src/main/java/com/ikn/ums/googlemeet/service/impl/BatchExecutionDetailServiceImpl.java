package com.ikn.ums.googlemeet.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.dto.BatchExecutionDetailDto;
import com.ikn.ums.googlemeet.repo.BatchExecutionDetailRepository;
import com.ikn.ums.googlemeet.service.BatchExecutionDetailService;
import com.ikn.ums.googlemeet.entity.*;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchExecutionDetailServiceImpl
        implements BatchExecutionDetailService {

    private final BatchExecutionDetailRepository repository;
    private final ModelMapper modelMapper;

    /**
     * Creates a new batch execution entry with IN_PROGRESS status.
     */
    @Override
    @Transactional(value = TxType.REQUIRED)
    public BatchExecutionDetailDto startBatch(String batchName) {

        BatchExecutionDetail entity = BatchExecutionDetail.builder()
                .batchName(batchName)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        BatchExecutionDetail saved = repository.save(entity);

        return modelMapper.map(saved, BatchExecutionDetailDto.class);
    }

    /**
     * Completes an existing batch execution and updates final status.
     */
    @Override
    @Transactional(value = TxType.REQUIRED)
    public BatchExecutionDetailDto completeBatch(
            BatchExecutionDetailDto batchDetailDto) {

        BatchExecutionDetail entity =
                modelMapper.map(batchDetailDto, BatchExecutionDetail.class);

        entity.setEndTime(LocalDateTime.now());

        if ("SUCCESS".equalsIgnoreCase(entity.getStatus()) || "PARTIAL_SUCCESS".equalsIgnoreCase(entity.getStatus())) {
            entity.setLastSuccessfulAt(LocalDateTime.now());
        }

        if (entity.getFailedUserEmails() == null) {
            entity.setFailedUserEmails(List.of());
        }

        BatchExecutionDetail saved = repository.save(entity);

        return modelMapper.map(saved, BatchExecutionDetailDto.class);
    }
}
