package com.unplan.unplanserver.domain.onboarding.service;

import com.unplan.unplanserver.domain.onboarding.dto.request.TransportRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.TransportResponse;
import com.unplan.unplanserver.domain.onboarding.entity.Transport;
import com.unplan.unplanserver.domain.onboarding.enums.TransportType;
import com.unplan.unplanserver.domain.onboarding.repository.TransportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportService {

    private final TransportRepository transportRepository;

    @Transactional
    public TransportResponse updateTransport(Long memberId, TransportRequest request) {

        transportRepository.deleteAllByMemberId(memberId);

        List<TransportType> transportTypes = new LinkedHashSet<>(request.transportTypes())
                .stream()
                .toList();

        List<Transport> transports = transportTypes.stream()
                .map(transportType -> Transport.builder()
                        .memberId(memberId)
                        .transportType(transportType)
                        .build())
                .toList();

        transportRepository.saveAll(transports);

        return new TransportResponse(memberId, transportTypes);
    }

    @Transactional(readOnly = true)
    public TransportResponse getTransport(Long memberId) {

        List<TransportType> transportTypes = transportRepository.findAllByMemberId(memberId)
                .stream()
                .map(Transport::getTransportType)
                .toList();

        return new TransportResponse(memberId, transportTypes);
    }
}