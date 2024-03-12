<%--
  Created by IntelliJ IDEA.
  User: qkqhe
  Date: 2024-03-12
  Time: 오후 7:27
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="utf-8">
    <title>접속자대기열시스템</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f5f5f5;
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
        }
        .message {
            text-align: center;
            padding: 20px;
            font-size: 18px;
            background-color: #fff;
            border-radius: 5px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }
    </style>
</head>
<body>
<div class="message">
    <h1>접속량이 많습니다.</h1>
    <span>현재 대기 순번 </span><span id="number">[[${number}]]</span><span> 입니다.</span>
    <br/>
    <p>서버의 접속량이 많아 시간이 걸릴 수 있습니다.</p>
    <p>잠시만 기다려주세요.</p>
    <p id="updated"></p>
    <br/>
</div>
<script>
    function fetchWaitingRank() {
        const queue = '[[${queue}]]';
        const userId = '[[${userId}]]';
        const queryParam = new URLSearchParams({queue: queue, user_id: userId});
        fetch('/api/v1/queue/rank?' + queryParam)
            .then(response => response.json())
            .then(data => {
                if(data.rank < 0) {
                    fetch('/api/v1/queue/touch?' + queryParam)
                        .then(response => {
                            document.querySelector('#number').innerHTML = 0;
                            document.querySelector('#updated').innerHTML = new Date();

                            const newUrl = window.location.origin + window.location.pathname + window.location.search;
                            window.location.href = newUrl;
                        })
                        .catch(error => console.error(error));
                    return;
                }
                document.querySelector('#number').innerHTML = data.rank;
                document.querySelector('#updated').innerHTML = new Date();
            })
            .catch(error => console.error(error));
    }

    setInterval(fetchWaitingRank, 3000);
</script>
</body>
</html>