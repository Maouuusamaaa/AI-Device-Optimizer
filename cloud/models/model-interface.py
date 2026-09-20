#!/usr/bin/env python3
"""Minimal model adapter interface for Stage 3; no model dependency required."""
from __future__ import annotations
from dataclasses import dataclass
from typing import Mapping, Protocol
@dataclass(frozen=True)
class ModelRecommendation:
    model_id: str
    action_id: str
    confidence: float
class AdvisorModel(Protocol):
    model_id: str
    def recommend(self, features: Mapping[str, float]) -> ModelRecommendation: ...

def validate_recommendation(value: ModelRecommendation) -> None:
    if not value.model_id.strip(): raise ValueError("model_id must be non-empty")
    if not value.action_id.strip(): raise ValueError("action_id must be non-empty")
    if not 0.0 <= value.confidence <= 1.0: raise ValueError("confidence must be 0..1")
