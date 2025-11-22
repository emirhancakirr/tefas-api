package com.tefasfundapi.tefasFundAPI.service;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import java.util.List;
import java.util.Optional;

public interface TefasService {
    Optional<FundDto> getFund(String code, List<String> fields);
}