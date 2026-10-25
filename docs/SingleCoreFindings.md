# Single-Threaded Baseline Performance Analysis

## Purpose
We use quantitative methods to measure the headroom trace under increasing FrameProcessor loads. The result will allow us to identify key points where single-threaded scheduling fails to maintain the frame rate.

## Experiment procedure
- This stress test will expand to 3 unique runs with (5, 6, 7) filters. Each will have 24 seconds to test.
- The Vignetter frame processor is used as a controll FrameProcessor, as other processors like `numberer` or `dusterer` are too light-weight.
- **A Python script** will be used to create a visual and investigate where performance begins to drop. These results will later be compared to our modified multi-threaded video player in the future.

## Results
## 5 Vignetter filters
```
# Ran with the command
VideoPlayer -xvvvvv "path\to\SimpsonsTrailer.mp4"
```
<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [32m 2769us[0m out of 41708us spare.
	Frame headroom is [31m-4503us[0m out of 41708us spare.
	Frame headroom is [32m 8158us[0m out of 41708us spare.
	Frame headroom is [31m -936us[0m out of 41708us spare.
	Frame headroom is [32m13679us[0m out of 41708us spare.
	Frame headroom is [32m 1094us[0m out of 41708us spare.
	Frame headroom is [32m 9518us[0m out of 41708us spare.
	Frame headroom is [32m 4405us[0m out of 41708us spare.
	Frame headroom is [32m12301us[0m out of 41708us spare.
	Frame headroom is [32m 5518us[0m out of 41708us spare.
	Frame headroom is [31m-2158us[0m out of 41708us spare.
	Frame headroom is [32m 4854us[0m out of 41708us spare.
	Frame headroom is [31m -918us[0m out of 41708us spare.
	Frame headroom is [32m10807us[0m out of 41708us spare.
	Frame headroom is [32m  386us[0m out of 41708us spare.
	Frame headroom is [32m 9379us[0m out of 41708us spare.
	Frame headroom is [32m 1066us[0m out of 41708us spare.
	Frame headroom is [31m-3557us[0m out of 41708us spare.
	Frame headroom is [32m 7407us[0m out of 41708us spare.
	Frame headroom is [31m-1113us[0m out of 41708us spare.
	Frame headroom is [32m10370us[0m out of 41708us spare.
	Frame headroom is [31m-1980us[0m out of 41708us spare.
	Frame headroom is [32m11337us[0m out of 41708us spare.
	Frame headroom is [32m 2046us[0m out of 41708us spare.
	
	Python data array `[2769, -4503, 8158, -936, 13679, 1094, 9518, 4405, 12301, 5518, -2158, 4854, -918, 10807, 386, 9379, 1066, -3557, 7407, -1113, 10370, -1980, 11337, 2046]`
</details>




### 6 Vignetter Filters
```
# Ran with the command
VideoPlayer -xvvvvvv "path\to\SimpsonsTrailer.mp4"
```

<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [31m-1819us[0m out of 41708us spare.
	Frame headroom is [32m 4775us[0m out of 41708us spare.
	Frame headroom is [31m -210us[0m out of 41708us spare.
	Frame headroom is [32m 6527us[0m out of 41708us spare.
	Frame headroom is [32m 1785us[0m out of 41708us spare.
	Frame headroom is [31m-4793us[0m out of 41708us spare.
	Frame headroom is [32m  471us[0m out of 41708us spare.
	Frame headroom is [31m-1615us[0m out of 41708us spare.
	Frame headroom is [32m 4194us[0m out of 41708us spare.
	Frame headroom is [31m -650us[0m out of 41708us spare.
	Frame headroom is [31m-10377us[0m out of 41708us spare.
	Frame headroom is [31m-2878us[0m out of 41708us spare.
	Frame headroom is [32m 5883us[0m out of 41708us spare.
	Frame headroom is [32m 1875us[0m out of 41708us spare.
	Frame headroom is [31m-4789us[0m out of 41708us spare.
	Frame headroom is [32m 3693us[0m out of 41708us spare.
	Frame headroom is [32m 6723us[0m out of 41708us spare.
	Frame headroom is [32m 3894us[0m out of 41708us spare.
	Frame headroom is [31m-1000us[0m out of 41708us spare.
	Frame headroom is [32m 4342us[0m out of 41708us spare.
	Frame headroom is [31m-3357us[0m out of 41708us spare.
	Frame headroom is [32m 4961us[0m out of 41708us spare.
	Frame headroom is [32m 2450us[0m out of 41708us spare.
	Frame headroom is [31m-3835us[0m out of 41708us spare.
	
	Python data array `[-1819, 4775, -210, 6527, 1785, -4793, 471, -1615, 4194, -650, -10377, -2878, 5883, 1875, -4789, 3693, 6723, 3894, -1000, 4342, -3357, 4961, 2450, -3835]`
	
</details>

### 7 Vignetter filters
```
# Ran with the command
VideoPlayer -xvvvvvvv "path\to\SimpsonsTrailer.mp4"
```

<details>
  <summary>Click to view full logs</summary>
  
	Frame headroom is [31m-119906us[0m out of 41708us spare.
	Frame headroom is [31m-117913us[0m out of 41708us spare.
	Frame headroom is [31m-79448us[0m out of 41708us spare.
	Frame headroom is [31m-36460us[0m out of 41708us spare.
	Frame headroom is [31m-2552us[0m out of 41708us spare.
	Frame headroom is [31m-2516us[0m out of 41708us spare.
	Frame headroom is [31m-6099us[0m out of 41708us spare.
	Frame headroom is [31m -509us[0m out of 41708us spare.
	Frame headroom is [32m 1187us[0m out of 41708us spare.
	Frame headroom is [31m-3646us[0m out of 41708us spare.
	Frame headroom is [31m-3105us[0m out of 41708us spare.
	Frame headroom is [31m-1769us[0m out of 41708us spare.
	Frame headroom is [31m -540us[0m out of 41708us spare.
	Frame headroom is [31m-3735us[0m out of 41708us spare.
	Frame headroom is [31m-5950us[0m out of 41708us spare.
	Frame headroom is [31m-4905us[0m out of 41708us spare.
	Frame headroom is [31m-2501us[0m out of 41708us spare.
	Frame headroom is [31m-10272us[0m out of 41708us spare.
	Frame headroom is [32m  373us[0m out of 41708us spare.
	Frame headroom is [31m  -51us[0m out of 41708us spare.
	Frame headroom is [31m-1820us[0m out of 41708us spare.
	Frame headroom is [31m-4572us[0m out of 41708us spare.
	Frame headroom is [31m-11513us[0m out of 41708us spare.
	Frame headroom is [31m-7688us[0m out of 41708us spare.
	
	Python data array `[-119906, -117913, -79448, -36460, -2552, -2516, -6099, -509, 1187, -3646, -3105, -1769, -540, -3735, -5950, -4905, -2501, -10272, 373, -51, -1820, -4572, -11513, -7688]`
	
</details>


## Findings

As shown below, of course the more filters the less consistent the frames are. 
| Filters | Mean headroom | Missed deadlines (us < 0) |
| --- | --- | --- |
| 5 Filters | +4164us | 7/29 (29%) |
| 6 Filters | +677us | 11/24 (46%) |
| 7 Filters | -17746us | 22/24 (92%) |

![Figure 1](../assets/Figure_1.png)

This reveals that:
- 5 Filters is marginally stable, having a nice positive mean and only missed a few deadlines. 
- 6 Filters oscillates aroun d the zero headroom, and the single CPU core is ~99% utilised.
- 7 Filters shows the massive cold-start spike, and stabalises after ~4 seconds, but still mostly not meeting the deadline.