package com.example.free2party.exception

sealed class PlanException(message: String) : Exception(message)

class PlanNotFoundException(message: String = "The plan you are looking for does not exist") :
    PlanException(message)

class OverlappingPlanException(message: String = "This time slot overlaps with an existing plan") :
    PlanException(message)

class InvalidPlanDataException(message: String = "The plan details are invalid") :
    PlanException(message)

class PastDateTimeException(message: String = "Cannot schedule plans for a past date or time") :
    PlanException(message)
