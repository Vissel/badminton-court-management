package com.badminton.backup.model;

public record TriggerResponse(long triggerId, TriggerStatus status, BackupScope scope) {}
