# Single-Threaded Baseline Performance Analysis

## Purpose
We use quantitative methods to measure the headroom trace under increasing FrameProcessor loads. The result will allow us to identify key points where multi-threaded core scheduling succeeds compared to single-threaded core scheduling..

## Experiment procedure
- This stress test will expand to 6 unique runs with (5, 6, 7) filters (to compare with single-threaded), and (46, 47, 48), the limits of multi-thread work. Each will have 24 seconds to test.
- The Vignetter frame processor is used as a control FrameProcessor, as other processors like `numberer` or `dusterer` are too light-weight.
- **A Python script** will be used to create a visual and investigate where performance begins to drop. These results will later be compared to our modified multi-threaded video player in the future.

## Results
## 5 Vignetter filters
```
# Ran with the command
VideoPlayer -xvvvvv "path\to\SimpsonsTrailer.mp4"
```
<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [32m235529us[0m out of 41708us spare.
	Frame headroom is [32m243875us[0m out of 41708us spare.
	Frame headroom is [32m238229us[0m out of 41708us spare.
	Frame headroom is [32m230886us[0m out of 41708us spare.
	Frame headroom is [32m240019us[0m out of 41708us spare.
	Frame headroom is [32m233155us[0m out of 41708us spare.
	Frame headroom is [32m241506us[0m out of 41708us spare.
	Frame headroom is [32m232080us[0m out of 41708us spare.
	Frame headroom is [32m243737us[0m out of 41708us spare.
	Frame headroom is [32m237926us[0m out of 41708us spare.
	Frame headroom is [32m245778us[0m out of 41708us spare.
	Frame headroom is [32m239183us[0m out of 41708us spare.
	Frame headroom is [32m231984us[0m out of 41708us spare.
	Frame headroom is [32m241818us[0m out of 41708us spare.
	Frame headroom is [32m234273us[0m out of 41708us spare.
	Frame headroom is [32m243395us[0m out of 41708us spare.
	Frame headroom is [32m235936us[0m out of 41708us spare.
	Frame headroom is [32m244462us[0m out of 41708us spare.
	Frame headroom is [32m238322us[0m out of 41708us spare.
	Frame headroom is [32m246714us[0m out of 41708us spare.
	Frame headroom is [32m239396us[0m out of 41708us spare.
	Frame headroom is [32m233141us[0m out of 41708us spare.
	Frame headroom is [32m242350us[0m out of 41708us spare.
	Frame headroom is [32m235343us[0m out of 41708us spare.
	
	Python data array `[235529, 243875, 238229, 230886, 240019, 233155, 241506, 232080, 243737, 237926, 245778, 239183, 231984, 241818, 234273, 243395, 235936, 244462, 238322, 246714, 239396, 233141, 242350, 235343]`
</details>




### 6 Vignetter Filters
```
# Ran with the command
VideoPlayer -xvvvvvv "path\to\SimpsonsTrailer.mp4"
```

<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [32m234794us[0m out of 41708us spare.
	Frame headroom is [32m245047us[0m out of 41708us spare.
	Frame headroom is [32m238003us[0m out of 41708us spare.
	Frame headroom is [32m247283us[0m out of 41708us spare.
	Frame headroom is [32m240163us[0m out of 41708us spare.
	Frame headroom is [32m232897us[0m out of 41708us spare.
	Frame headroom is [32m242983us[0m out of 41708us spare.
	Frame headroom is [32m234626us[0m out of 41708us spare.
	Frame headroom is [32m241670us[0m out of 41708us spare.
	Frame headroom is [32m237265us[0m out of 41708us spare.
	Frame headroom is [32m246719us[0m out of 41708us spare.
	Frame headroom is [32m239963us[0m out of 41708us spare.
	Frame headroom is [32m233235us[0m out of 41708us spare.
	Frame headroom is [32m242655us[0m out of 41708us spare.
	Frame headroom is [32m234162us[0m out of 41708us spare.
	Frame headroom is [32m244088us[0m out of 41708us spare.
	Frame headroom is [32m236480us[0m out of 41708us spare.
	Frame headroom is [32m245687us[0m out of 41708us spare.
	Frame headroom is [32m238042us[0m out of 41708us spare.
	Frame headroom is [32m231635us[0m out of 41708us spare.
	Frame headroom is [32m240937us[0m out of 41708us spare.
	Frame headroom is [32m233669us[0m out of 41708us spare.
	Frame headroom is [32m242610us[0m out of 41708us spare.
	Frame headroom is [32m234919us[0m out of 41708us spare.
	
	Python data array `[234794, 245047, 238003, 247283, 240163, 232897, 242983, 234626, 241670, 237265, 246719, 239963, 233235, 242655, 234162, 244088, 236480, 245687, 238042, 231635, 240937, 233669, 242610, 234919]`
	
</details>

### 7 Vignetter filters
```
# Ran with the command
VideoPlayer -xvvvvvvv "path\to\SimpsonsTrailer.mp4"
```

<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [32m235667us[0m out of 41708us spare.
	Frame headroom is [32m245516us[0m out of 41708us spare.
	Frame headroom is [32m238240us[0m out of 41708us spare.
	Frame headroom is [32m231160us[0m out of 41708us spare.
	Frame headroom is [32m241451us[0m out of 41708us spare.
	Frame headroom is [32m234288us[0m out of 41708us spare.
	Frame headroom is [32m243277us[0m out of 41708us spare.
	Frame headroom is [32m236262us[0m out of 41708us spare.
	Frame headroom is [32m242678us[0m out of 41708us spare.
	Frame headroom is [32m238351us[0m out of 41708us spare.
	Frame headroom is [32m231027us[0m out of 41708us spare.
	Frame headroom is [32m239602us[0m out of 41708us spare.
	Frame headroom is [32m233395us[0m out of 41708us spare.
	Frame headroom is [32m242027us[0m out of 41708us spare.
	Frame headroom is [32m235319us[0m out of 41708us spare.
	Frame headroom is [32m244026us[0m out of 41708us spare.
	Frame headroom is [32m237230us[0m out of 41708us spare.
	Frame headroom is [32m246616us[0m out of 41708us spare.
	Frame headroom is [32m239314us[0m out of 41708us spare.
	Frame headroom is [32m232399us[0m out of 41708us spare.
	Frame headroom is [32m240708us[0m out of 41708us spare.
	Frame headroom is [32m234065us[0m out of 41708us spare.
	Frame headroom is [32m243176us[0m out of 41708us spare.
	Frame headroom is [32m236575us[0m out of 41708us spare.
	
	Python data array `[235667, 245516, 238240, 231160, 241451, 234288, 243277, 236262, 242678, 238351, 231027, 239602, 233395, 242027, 235319, 244026, 237230, 246616, 239314, 232399, 240708, 234065, 243176, 236575]`
	
</details>

## 46 Vignetter filters
```
# Ran with the command
VideoPlayer -xvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv "path\to\SimpsonsTrailer.mp4"
```
<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [32m43828us[0m out of 41708us spare.
	Frame headroom is [32m59198us[0m out of 41708us spare.
	Frame headroom is [32m86325us[0m out of 41708us spare.
	Frame headroom is [32m92669us[0m out of 41708us spare.
	Frame headroom is [32m111570us[0m out of 41708us spare.
	Frame headroom is [32m144323us[0m out of 41708us spare.
	Frame headroom is [32m200617us[0m out of 41708us spare.
	Frame headroom is [32m228252us[0m out of 41708us spare.
	Frame headroom is [32m233812us[0m out of 41708us spare.
	Frame headroom is [32m242265us[0m out of 41708us spare.
	Frame headroom is [32m232676us[0m out of 41708us spare.
	Frame headroom is [32m243155us[0m out of 41708us spare.
	Frame headroom is [32m232623us[0m out of 41708us spare.
	Frame headroom is [32m245961us[0m out of 41708us spare.
	Frame headroom is [32m237657us[0m out of 41708us spare.
	Frame headroom is [32m231945us[0m out of 41708us spare.
	Frame headroom is [32m237354us[0m out of 41708us spare.
	Frame headroom is [32m233210us[0m out of 41708us spare.
	Frame headroom is [32m240487us[0m out of 41708us spare.
	Frame headroom is [32m231013us[0m out of 41708us spare.
	Frame headroom is [32m233505us[0m out of 41708us spare.
	Frame headroom is [32m237411us[0m out of 41708us spare.
	Frame headroom is [32m241748us[0m out of 41708us spare.
	Frame headroom is [32m239271us[0m out of 41708us spare.
		
	Python data array `[43828, 59198, 86325, 92669, 111570, 144323, 200617, 228252, 233812, 242265, 232676, 243155, 232623, 245961, 237657, 231945, 237354, 233210, 240487, 231013, 233505, 237411, 241748, 239271]

</details>


### 47 Vignetter Filters
```
# Ran with the command
VideoPlayer -xvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv "path\to\SimpsonsTrailer.mp4"
```

<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [32m42687us[0m out of 41708us spare.
	Frame headroom is [32m77164us[0m out of 41708us spare.
	Frame headroom is [32m83986us[0m out of 41708us spare.
	Frame headroom is [32m71360us[0m out of 41708us spare.
	Frame headroom is [32m52551us[0m out of 41708us spare.
	Frame headroom is [32m78636us[0m out of 41708us spare.
	Frame headroom is [32m56406us[0m out of 41708us spare.
	Frame headroom is [32m47850us[0m out of 41708us spare.
	Frame headroom is [32m64927us[0m out of 41708us spare.
	Frame headroom is [32m78224us[0m out of 41708us spare.
	Frame headroom is [32m86597us[0m out of 41708us spare.
	Frame headroom is [32m97113us[0m out of 41708us spare.
	Frame headroom is [32m90249us[0m out of 41708us spare.
	Frame headroom is [32m82902us[0m out of 41708us spare.
	Frame headroom is [32m54398us[0m out of 41708us spare.
	Frame headroom is [32m12965us[0m out of 41708us spare.
	Frame headroom is [31m -677us[0m out of 41708us spare.
	Frame headroom is [31m-13482us[0m out of 41708us spare.
	Frame headroom is [31m-14379us[0m out of 41708us spare.
	Frame headroom is [31m-38805us[0m out of 41708us spare.
	Frame headroom is [31m-26698us[0m out of 41708us spare.
	Frame headroom is [31m-29551us[0m out of 41708us spare.
	Frame headroom is [31m-15804us[0m out of 41708us spare.
	Frame headroom is [31m-36250us[0m out of 41708us spare.
	
	Python data array `[42687, 77164, 83986, 71360, 52551, 78636, 56406, 47850, 64927, 78224, 86597, 97113, 90249, 82902, 54398, 12965, -677, -13482, -14379, -38805, -26698, -29551, -15804, -36250]`
	
</details>

### 48 Vignetter filters
```
# Ran with the command
VideoPlayer -xvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv "path\to\SimpsonsTrailer.mp4"
```

<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [31m-50829us[0m out of 41708us spare.
	Frame headroom is [31m-79553us[0m out of 41708us spare.
	Frame headroom is [31m-127931us[0m out of 41708us spare.
	Frame headroom is [31m-182359us[0m out of 41708us spare.
	Frame headroom is [31m-197472us[0m out of 41708us spare.
	Frame headroom is [31m-194247us[0m out of 41708us spare.
	Frame headroom is [31m-208205us[0m out of 41708us spare.
	Frame headroom is [31m-215772us[0m out of 41708us spare.
	Frame headroom is [31m-211136us[0m out of 41708us spare.
	Frame headroom is [31m-267203us[0m out of 41708us spare.
	Frame headroom is [31m-343054us[0m out of 41708us spare.
	Frame headroom is [31m-448780us[0m out of 41708us spare.
	Frame headroom is [31m-439123us[0m out of 41708us spare.
	Frame headroom is [31m-504495us[0m out of 41708us spare.
	Frame headroom is [31m-582440us[0m out of 41708us spare.
	Frame headroom is [31m-587274us[0m out of 41708us spare.
	Frame headroom is [31m-597565us[0m out of 41708us spare.
	Frame headroom is [31m-627866us[0m out of 41708us spare.
	Frame headroom is [31m-668647us[0m out of 41708us spare.
	Frame headroom is [31m-758272us[0m out of 41708us spare.
	Frame headroom is [31m-831755us[0m out of 41708us spare.
	Frame headroom is [31m-838539us[0m out of 41708us spare.
	Frame headroom is [31m-875832us[0m out of 41708us spare.
	Frame headroom is [31m-930249us[0m out of 41708us spare.
		
	Python data array `[-50829, -79553, -127931, -182359, -197472, -194247, -208205, -215772, -211136, -267203, -343054, -448780, -439123, -504495, -582440, -587274, -597565, -627866, -668647, -758272, -831755, -838539, -875832, -930249]`
	
</details>

## Findings (First three)

As shown below, multi-threaded scheduling vastly out performs single-threaded scheduling. 
| Filters | Mean headroom | Missed deadlines (us < 0) | Absolute Delta
| --- | --- | --- | --- |
| 5 Filters | +238709us | 0/24 (0%) | +234,545us |
| 6 Filters | +239147us | 0/24 (0%) | +238,470us |
| 7 Filters | +238432us | 0/24 (0%) | +256,178us |

![Figure 2](/assets/Figure_2.png)

The limits of multi-threaded still exists. If we were to use the Vignetter frame for reference, it exists around 46, 47 and 48 filters

![Figure 3](/assets/Figure_3.png)

