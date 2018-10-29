# eZEX DTH for SmartThings
- 이젝스(http://www.ezex.co.kr)사의 전등스위치/콘센트 DTH(Device Type Handler)를 제공합니다.
- 이곳에서 배포하는 소스는 'SmartThings & Connect Home' 커뮤니티(http://cafe.naver.com/stsmarthome)에서 제작한 것으로, 이젝스에서 공식으로 배포하는 DTH(https://github.com/ezexcorp/smartthings)와는 다릅니다.

# 전등스위치 DTH 설치시 주의사항
- 2구 이상 전등 스위치는 'eZEX Light Switch Child Device (STS)' DTH도 반드시 같이 설치 해주시기 바랍니다.
- 1구 전등 스위치는 SmartThings의 'ZigBee Switch'를 사용하시기 바랍니다.

# 업데이트 내역
## 1.0.2 (2018.10.29)
 - SmartThings App(클래식 앱이 아닌)에서 켜고 끄기를 지원합니다.
 - 1구 DTH를 삭제하였습니다. (기본으로 제공하는 'ZigBee Switch' DTH에 비해 장점이 없습니다)
 - 5구 DTH를 추가하였습니다.
 - DTH Type을 변경할 때 정상적으로 전등 버튼(Child Device)이 만들어지지 않는 문제를 수정하였습니다.
 
