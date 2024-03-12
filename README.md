

redis-cli 테스트


while [ true ]; do date; redis-cli zcard users:queue:default:wait; redis-cli zcard users:queue:default:proceed; sleep 1; done;
