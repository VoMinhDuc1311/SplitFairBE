package com.anygroup.splitfair.service;

import com.anygroup.splitfair.dto.VietQrDTO;
import com.anygroup.splitfair.model.Debt;

public interface VietQrService {
    VietQrDTO generateDebtVietQr(Debt debt);
}
