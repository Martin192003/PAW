<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Booking Details</title>
</head>
<body>
<h1>Booking Details</h1>
<p>Booking ID: ${booking.bookingId}</p>
<c:if test="${hasReceipt}">
    <p>Receipt uploaded.</p>
    <a href="/bookings/${booking.bookingId}/receipt">View Receipt</a>
</c:if>
<c:if test="${!hasReceipt}">
    <p>No receipt uploaded yet.</p>
</c:if>
</body>
</html>

