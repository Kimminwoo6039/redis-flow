

redis-cli 테스트


while [ true ]; do date; redis-cli zcard users:queue:default:wait; redis-cli zcard users:queue:default:proceed; sleep 1; done;




## Apache JMeter 설치 및 실행
- https://jmeter.apache.org/ 에 접속한다.
- 좌측 메뉴에 Download Releases를 클릭한다.
- Binaries > zip파일을 다운로드 한다.
- 적당한 위치에 압축을 해제한 후, bin폴더에 jmeter.bat을 실행한다.
- cmd 창과 함께 Apache Jmeter 창이 뜨면 실행이 된 것이다.
