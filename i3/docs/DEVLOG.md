# i3 개발 로그

각 항목은 코드가 아니라 **증거**를 남긴다. 무엇을 만들었는지가 아니라 무엇을 실행해서 무엇을
봤는지를 쓴다. 캡처 절차는 [evidence/README.md](evidence/README.md), 페이즈 범위는
[PLAN.md](PLAN.md).

## 2026-08-29 · P1-A 레이아웃 산술, 상태 모델, 포커스 링

게임 클래스를 한 줄도 참조하지 않는 다섯 파일로 시작했다. 위젯을 그리기 전에 위젯이 서 있을
좌표계와 "지금 어떤 상태인가"를 먼저 확정해야 하고, 그 둘은 마인크래프트 없이 JUnit으로 전수
검증이 가능한 유일한 부분이다.

- `layout/Box` — 이 클라이언트에서 공간을 나누는 유일한 산술. 모든 메서드가 새 박스를 반환하고,
  뒤집힌 좌표쌍은 음수가 아니라 0으로 붕괴한다. `col`/`row`의 나눗셈 나머지는 앞쪽 열에 1px씩
  분배한다 — 마지막 열에 몰아주면 오른쪽 끝이 삐뚤어진다.
- `layout/Stack` — 밴드 커서. **의도적으로 클램프하지 않는다.** 넘치는 밴드를 영역 안으로
  접어 넣으면 화면이 조용히 틀어지고, 넘침은 `overflows()`로 잡아서 다시 배치해야 할 사건이다.
- `ui/State` — 8상태(rest/hover/active/focus/focus-visible/disabled/loading/error)와 `Signals`
  → `State` 우선순위 하나. 우선순위를 위젯마다 두면 같은 화면의 두 위젯이 자기 상태를 다르게
  판정한다. `FOCUS`와 `FOCUS_VISIBLE`을 분리한 이유는 도착 경로가 다르기 때문이다: 클릭은 링
  없이 포커스를 남기고, Tab은 링을 켜야 한다.
- `ui/Chrome` — 상태 → (fill, ink, line) 토큰 매핑. 목소리는 `quiet`와 `loud` **둘뿐이고**,
  loud가 두 개 있는 화면은 자기가 무엇을 위한 화면인지 결정하지 못한 화면이다.
- `ui/Focus` — 순회 링. 위치와 가시성이 별개 상태다. `advance(step)`은 링을 켜고 양방향으로
  래핑하며 포커스를 받지 않는 대상을 건너뛴다. `point(target)`은 링을 켜지 않는다(클릭 경로).
  포커스를 쥔 컨트롤이 도중에 disabled가 되면 `held()`는 null을 반환하지만 **위치는 남긴다** —
  다시 활성화되면 그 자리로 돌아온다.

### 증거

`gradlew -p i3/mod build test --console=plain` → `BUILD SUCCESSFUL`. 테스트 47개, 실패 0:

| 클래스 | tests | failures |
| --- | --- | --- |
| `layout.BoxTest` | 10 | 0 |
| `layout.StackTest` | 7 | 0 |
| `ui.StateTest` | 5 | 0 |
| `ui.FocusTest` | 17 | 0 |
| `ui.ChromeTest` | 8 | 0 |

초록불 자체는 증거가 약하므로 **뮤테이션 한 번**을 돌렸다. `Box.col`에서 나머지 분배
(`Math.min(index, spare)`)를 빼고 `--tests BoxTest`를 실행:

```
> Task :test FAILED

BoxTest > columnsTileTheBoxExactly() FAILED
    org.opentest4j.AssertionFailedError at BoxTest.java:76

10 tests completed, 1 failed
```

즉 타일링 테스트는 실제로 물고 있다. 원본 복원 후 전체 재실행도 `BUILD SUCCESSFUL`.

`ChromeTest.aPressDarkensRatherThanLifts`는 hex를 눈으로 비교하지 않고 상대 휘도를 계산한다 —
`accent.pressed`가 `accent`보다 어둡다는 건 측정값이어야 하고, quiet은 어둡게 할 fill이 없어서
`accent.wash`로 올라간다는 것도 같은 함수로 확인한다.

### 아직 아닌 것

- 캡처 없음. P1-A는 픽셀을 하나도 그리지 않는다 — 위젯이 없으므로 상태 매트릭스 캡처는 P1-B
  이후다.
- `Focus`는 아직 어떤 서피스에도 연결되지 않았다. 키보드 순회 캡처는 P1-D 몫이다.

## 2026-08-29 · P1-B 서피스 이벤트 라우팅, 버튼과 스위치

포인터와 키보드를 받는 물건이 처음 생겼다. 규칙은 컨트롤이 아니라 `ui/Surface` 한 곳에 있고,
`Surface`는 **그리지 않는다** — 그리는 순서는 레이아웃의 몫이다(팝오버는 자기를 띄운 행 위에
칠해야 하고, 둘을 동시에 아는 건 레이아웃뿐이다). 그래서 게임 클래스가 이 경로에 하나도 없고,
`InputConstants`의 키 번호는 javac가 바이트코드에 접어 넣는 상수라 헤드리스 JUnit에서 클래스
로딩 없이 규칙을 전수 검증할 수 있다.

- `ui/Surface` — 등록 순서가 Tab 순서, 나중에 등록한 위젯이 포인터를 먼저 먹는다. 캡처는
  누른 위젯이 쥐고, 눌린 상태는 포인터를 따라 경계를 드나든다. 죽은 컨트롤은 클릭을
  **삼킨다** — 뒤로 흘려보내면 컨트롤이 꺼져 있다는 이유로 플레이어의 조준을 벌하는 셈이다.
- `ui/Button`, `ui/Toggle` — `draw(Painter, State)` 하나로 여덟 상태를 전부 그린다.
- 링이 상태에서 빠져나왔다. `State`는 한 값이라 "지금 참인 것 중 가장 큰 것"만 말할 수 있는데,
  포커스는 포인터가 도착하거나 요청이 나가는 순간 그 경쟁에서 진다. 링은 서피스가 소유하는
  별개의 비트다.

### 증거

`gradlew -p i3/mod clean build test --console=plain` → `BUILD SUCCESSFUL in 3s`. 테스트 72개,
실패 0:

| 클래스 | tests | failures |
| --- | --- | --- |
| `layout.BoxTest` | 10 | 0 |
| `layout.StackTest` | 7 | 0 |
| `text.TypesetTest` | 6 | 0 |
| `ui.StateTest` | 5 | 0 |
| `ui.FocusTest` | 17 | 0 |
| `ui.SurfaceTest` | 18 | 0 |
| `ui.VoiceTest` | 9 | 0 |

뮤테이션 한 번. `Surface.key`의 `live()` 게이트를 빼서 요청이 떠 있는 컨트롤도 Enter에 반응하게
되돌리고 `--tests '*SurfaceTest'`:

```
> Task :test FAILED

SurfaceTest > aControlInFlightKeepsTheRingAndAnswersNothing() FAILED
    org.opentest4j.AssertionFailedError at SurfaceTest.java:280

18 tests completed, 1 failed
```

원본 복원 후 재실행 `BUILD SUCCESSFUL`. 캡처는 [evidence/README.md](evidence/README.md)의 P1-B
표 세 개 — 매트릭스 32칸, 마우스 없는 Tab 4정거장, `loading` 대 `disabled` 2배 크롭.

캡처 절차 자체를 `tools/capture.py`로 굳혔다. Xvfb, Gradle 런, 아틀라스 대기, XTEST 탭, 루트
윈도우 `import`이 손으로는 여섯 번 틀릴 수 있는 일이었고, 순회 캡처는 정거장마다 **같은
서피스**여야 의미가 있어서 샷 사이에 화면을 다시 열지 않는 규칙이 도구에 들어가 있다.

### 캡처가 잡은 것

- 마스트헤드의 액센트 바가 워드마크 **옆**이 아니라 **아래**에 있었다. 베이스라인은 어떤
  프로바이더든 드로 원점 +7px인데 바를 역할의 공칭 박스로 재고 있었으니, 22px 페이스는 몸통
  대부분을 원점 위에 그리고 바는 베이스라인에 걸린다. `Typeset.capTop`/`capHeight`가 대문자
  밴드를 이름 붙이고 바는 거기서 잰다. 픽셀로 확인: 틱 28..61행, 워드마크 잉크 26..61행 —
  아래가 정확히 맞고 위는 1 gui px 안쪽(세리프 오버슛)이다.
- 스위치의 `loading`이 `disabled`와 사실상 같은 그림이었다. 버튼은 라벨을 점 셋으로
  바꾸는데 스위치는 노브 밝기만 달랐다. 요청이 떠 있는 스위치는 **위치 사이**에 있는 것이므로
  노브를 트랙 중앙에 세운다.

### 아직 아닌 것

- `SpecimenScreen`은 아직 자기 마스트헤드 사본을 들고 있어서 커밋된 P0 캡처에 위 버그가 남아
  있다. `DevChrome`으로 옮기는 건 두 화면이 탭 레일을 공유하는 P1-D에서 같이 한다.
- 슬라이더·셀렉트·텍스트필드가 없으니 드래그와 캐럿 경로는 `Surface`에서 아직 죽은 코드에
  가깝다. `scroll`도 받는 위젯이 없다.
