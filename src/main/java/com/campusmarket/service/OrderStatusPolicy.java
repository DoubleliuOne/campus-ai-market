package com.campusmarket.service;

import com.campusmarket.exception.BusinessException;

public final class OrderStatusPolicy {

    private OrderStatusPolicy() {
    }

    public static void validateTransition(String currentStatus,
                                          String targetStatus,
                                          boolean buyer,
                                          boolean seller) {
        if (currentStatus == null || targetStatus == null) {
            throw new BusinessException("订单状态不能为空");
        }
        if (!buyer && !seller) {
            throw new BusinessException("无权操作该订单");
        }

        switch (currentStatus) {
            case "CREATED" -> {
                if ("CONFIRMED".equals(targetStatus)) {
                    requireSeller(seller);
                    return;
                }
                if ("CANCELLED".equals(targetStatus)) {
                    return;
                }
            }
            case "CONFIRMED" -> {
                if ("IN_PROGRESS".equals(targetStatus)) {
                    requireSeller(seller);
                    return;
                }
                if ("CANCELLED".equals(targetStatus)) {
                    return;
                }
            }
            case "IN_PROGRESS" -> {
                if ("COMPLETED".equals(targetStatus)) {
                    requireSeller(seller);
                    return;
                }
            }
            case "PAID" -> {
                if ("IN_PROGRESS".equals(targetStatus) || "COMPLETED".equals(targetStatus)) {
                    requireSeller(seller);
                    return;
                }
            }
            default -> {
                // Completed and cancelled orders are terminal.
            }
        }

        throw new BusinessException("不允许从"
                + orderStatusText(currentStatus)
                + "变更为"
                + orderStatusText(targetStatus));
    }

    private static void requireSeller(boolean seller) {
        if (!seller) {
            throw new BusinessException("只有卖家可以执行该订单操作");
        }
    }

    private static String orderStatusText(String status) {
        return switch (status) {
            case "CREATED" -> "待确认";
            case "CONFIRMED" -> "已确认";
            case "IN_PROGRESS" -> "交易中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "PAID" -> "已支付";
            default -> status;
        };
    }
}
