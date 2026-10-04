package com.example.rms.requirement.application;
import com.example.rms.shared.domain.Paging;
public record RequirementQuery(Paging paging,String keyword,String status,String kind,String level,String priority,Long tagId) {}
