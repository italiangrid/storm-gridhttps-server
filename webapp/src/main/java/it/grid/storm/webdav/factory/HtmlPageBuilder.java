package it.grid.storm.webdav.factory;

public class HtmlPageBuilder {

	public static String getFolderIco() {
		String out = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAAGXcA1uAAAABGdBTUEAALGO" +
				"fPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAACFklEQVR4nGL4//8/Aww" +
				"DBBBDSkpKKRDvB2GAAAJxfIH4PwgDBBADsjKAAALJsMKUAQQQA0wJCAMEEIhTiCQwHYjngrQABBBIIgtZJQwDBB" +
				"DYQCCjAIgbkLAmQACh2IiMAQII2cKtQDwbiDtAEgABBJNYB3MaFEcABBDMDgzLAQIIJKiPRaIOIIBgOpBd1AASA" +
				"wggmEQ4SBU0tCJwuRSEAQIIpLgam91Y8CaQBoAAggVGCJGa/gMEEEhDCi4fYsGXAAII5odiIjVIAwQQTs/hwgAB" +
				"RLIGgAACOUcY6vEqIC4DYlF8GgACCJdnpwLxTGiimAPEFjANAAFEbOj8h2kACCCYhjwgTkdLUeh4A0gDQADBNGg" +
				"RaVMpQACBNBwB4jYiNagDBBBIQwYQ3yLWHwABBNIgQIrHAQIIZ07Bgv+A1AIEEEyDUgpaZkHDebBgBQggkpMGQA" +
				"CRrIFUDBBAIOfLAvEbAv49B8Ss5FgAEEAgC+qIjQUi8F8gtke2ACCAQBZUICn4lwIpeX2AWAeINaC0FpQNwmpAr" +
				"IKEQRGoAMVyQCyJbAFAACGXFheAmAmIV1LRR+4AAQSyIBeIfwExHxDHUNFwEA4ACCCQBaBcPAWaXjdQ0fAfQMwC" +
				"EEAgQxOB2AaIOYD4DxUtWAFyNEAAwSMDKBBJ5eAJAZkLEEDIFmgD8TcqGAxKqmUwcwECiOY5GSCAaG4BQIABAFb" +
				"NMXYg1UnRAAAAAElFTkSuQmCC";
		return out;
	}

//	public static String getMiltonLogo() {
//		String out = "data:;base64,iVBORw0KGgoAAAANSUhEUgAAAQoAAABGCAYAAAA0JtpqAAAAGXRFWHRTb2Z0d2FyZQBBZ" +
//				"G9iZSBJbWFnZVJlYWR5ccllPAAAAyJpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i" +
//				"77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM" +
//				"6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMC1jMDYwIDYxLjEzNDc3NywgMjAxMC8wMi8xMi0xNz" +
//				"ozMjowMCAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyL" +
//				"XJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8v" +
//				"bnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21" +
//				"tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIH" +
//				"htcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENTNSBNYWNpbnRvc2giIHhtcE1NOkluc3RhbmNlSUQ9I" +
//				"nhtcC5paWQ6RDg3MTVFNUI0Mzk2MTFFMkE4RDVGQUNEODk3NENFODYiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5k" +
//				"aWQ6RDg3MTVFNUM0Mzk2MTFFMkE4RDVGQUNEODk3NENFODYiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW5" +
//				"zdGFuY2VJRD0ieG1wLmlpZDpEODcxNUU1OTQzOTYxMUUyQThENUZBQ0Q4OTc0Q0U4NiIgc3RSZWY6ZG9jdW1lbn" +
//				"RJRD0ieG1wLmRpZDpEODcxNUU1QTQzOTYxMUUyQThENUZBQ0Q4OTc0Q0U4NiIvPiA8L3JkZjpEZXNjcmlwdGlvb" +
//				"j4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/PgE1L9QAABk0SURBVHja7F0JfBRF" +
//				"uq+ayeS+JgmQwxyAghAYYEMQCWgQHoLyngcCgr913xN/63vA897HeizqrriAwnogrseP9UK5dD04goAEuSL8EDI" +
//				"GCSBkgCRAQjKTTM5Jput193T3VPf0ORmWBOqPSCZTXV39VX3/+r6vvqqGCCFAQEBAoAYTEQEBAQEhCgICgquTKM" +
//				"rKyiw9sW4CgqsVsLvFKN5Y+daDVHyYeVDvvgcmT5p8NJR1F31XlHvs4ulRZjflfXTu/I9J9xMQ9ECL4s2VKx6My" +
//				"Ihvi4qNWnWspmJU0dai3JCRxFYfSUTFx6yypMe2Mfci3U9A0MMsCkZxwzPi2gACawH9P6ZVbU0tDw1Mzjl0x5Q7" +
//				"7F2pe0vRltzjlxyjImgCggByTw5mtlc1Rj4293+JZUFA0BMsCsbdCE9nSAKt9VEErc4QgsjY6FXH6xx5jKIHTRJ" +
//				"btuSWX6rwkQRdp69++v/0vRjrhbk3GQYEBD2AKNojvTH0P2tpduB+g9j/mE8MWTCKvnnLZpvReplryusqRkXFxf" +
//				"hIQrCefETEkIUnyhtJhgEBQQ8gijEDR37TWF8/l9ZcVoEZRUbc3M8gKjaGtizOGLIsGEuCsUYYomEJAvew6Fswv" +
//				"2tyuh65ecDITWQYEBD0AKIYWzC2ilbYb9y04kq/g5yVwQQ4yy85RpWUlPTSqo8pw1ghrCXBxSQY2mHqgpzBwpME" +
//				"c28yDAgIegBRMBg3dhxDFpsa65xz/RO/z13gbYuouOhVlVVVA7TqYspExnGWhM+AEOpifmKsF0ISBAQ9kCh4y0J" +
//				"wQwAQCAL6TAugd4WGLu/lrRHchYEcSYwZmP8NIQkCgh5KFDxZpEWknIJcwJF1Fjj3Q1ja1ABNDGb2X8GigIC/ND" +
//				"UipYKQBAFBDycKBvHxCXWCHQABFogMLucDAX8wMz4uvp50OwHBVUAUtCVgZq0A5LcKQBCJYXxswp8/wbojXtLtB" +
//				"ATGENatWwf97gZC+l0PIT7BLrcCISfDwOUEBvFRWf1k3ui7OS1824CUWBEhH6ttshy44JnAW4i/G5pURKSmjY/L" +
//				"nJN5i/hKyqxbEgUz67PLmShIZwMhMVlgcQ7GWiHDL7T49oRz+LvFriX0j2xSXPgE6/gBKaAYL7O7uv0/PtzdsIH" +
//				"7aM+KM58cn5NwSq3ed0vrpz0yLOmLa1WuG0+6hv+92MnL1W4xg47Zg5N2ENdD5Hqwmu53H/gkLD0uCLZCArkpjF" +
//				"1mJad5XUbjD9p4eSuSsV/8qlm2u864sgo+O1P6UUnDwmtZpkgsK9uVbEu3tCgQhYA/nVucMKUHFEWZoZmLcQDE0" +
//				"QwX1KSIUl+GAW3mY0B44FhM/pQQmIYqS92v/Fj76MaD7jmsYoRD+7UsV4qL1QmypRAhCrn4gjBDscrOBTYNWAX8" +
//				"dcIAxYROEDrEhcM6mGKx0/1GMYJOCIe1KqYHl9si3w9l9R0FDElAElAC8bRcQUqY3cTIlR73iRGmWkIUgaas36B" +
//				"AnBuC/DkSRggH4sQByQAMNcbnJDr25iQO09WfCp/lSB4gSOSqIddrnij8XhpUNVUVJi2yBNr9mF8xYEHQ/dFdg5" +
//				"m+GYUnCGSMBPDMTOl+ERLQvFJ9qp8miNvR/RA2ZsXpUn8cAEtMwv15BGRzEPT4/EbjAkz59siMyDkpLsFtELfJg" +
//				"OuBPRf/IIsuZXzy6orTbcG0C2BtUfqd2rNrld83v59hM3PTSaeNqTUMAs+k663l/O9/qnLFbXB4nqho7Mx1tqPe" +
//				"qdGmszckmA89d1PvN5Xq2lBeX/B9lWdWZYu3f4QJtA5ICDs02GopeSBXe0luM90OzkMEd1xvtctbh2K3UHT9r04" +
//				"2qn+hhcrCxx3/e4ZoMmLgqeHpic1q7fjquHPU8YbOvHJXZ35dG5U2IDHs8BBr2N6hyWF789ITXHrlyt/XhIB38g" +
//				"1W4ezW8ouN5i8q2uadoO9R04qyMmPMJ4YlmYuHp1h2FmQnXgi1gvJyZXDnDVbdwV1WDq6OvPIGb/6lVipjoDXs0" +
//				"FCr5YchyeYSI3LAXQ+bKGKNzepCRiOUUQBulsazHoWINkABiqpXKfkAJL8/w08WQHcdrNUB/RaFv1725wH87+QU" +
//				"We4eSuXEJBRYVlqXRvsNR/gZxV6+3bWS6UMYabLTRMESzb0bK7++4PDkAGxJrZH+ewKA4ZsONc25ZWjMV4vH9Xm" +
//				"B/+67X503vrSncS1q8rLLnHw7q4Hnnp0A2VeWNoGlBfG3KynCNyeceYu/c67i7md3FlJPPjAkeYdULgGTD4ev6e" +
//				"uXcNf7+4Yu0olsLxc5S/lywwdFv70yHcyXa8PLJRef2Gxv+U/gQb5n8GXogUtn2yfv893fDuJd4KWx8dMn9rOeU" +
//				"JXrcWfB8m1OVq7AAu00UbByvX9z9YZzp9tvoJ/Bxj9LAwCFZQD8fjX93DDWBeaNinty9uDkkOQ6SOXq8qAntUh7" +
//				"UUnNo7Qc5iAPu7wn9P++c+2TaDk8w9QD4520HBI05SAbowiY7aDywObJQ2qB+OOPUBTZFsgCIXG2pYKS8qQAEQx" +
//				"UUr0p2AgI1gi7uQz52scTmRwp6CEJ+aAbCJCHSshFtlwXzG0bHgO47YvK/W3nPdG4wmPytjHLwz+UNoOnqBrLsl" +
//				"t7P/te6aVpH+5pXEh/aZO2i/vZBuq94A+bnVs/uNc0cnCf+A6ldnDX2+QeRVWGvmVAG14O6YpzALDttHPAi3vc6" +
//				"0EjOzfYEDY+Jfe3MWUWbnau3zikfdvrhalPa/hJIrkWfHSmFLm9IuWTjBEbaqLAiu8blrva0eK5I1I+D5FRgeVR" +
//				"KMuQlwNq7Axoo2TCtqFGCryw2aUtB5woFGdAHe5GwKBSuEba0XKDRlZxoDimwC2RGsis9B9UA7goOl+fHktHtDS" +
//				"rNuhlnku2bqj8rF1ZtmWf0otAwbpzpaiG0+MEs31MdsSmjBjzybNN3hsPnG2bDBson8LQA2n/0SbwTgI88um+pu" +
//				"dYkjBBe0pmRDVtpu8JM4GO4ur2GR01HRaaWFjCgV5oe/6ge8OXU+PvUgg5YzkrUJlUZUgxJ858dNSQ6KVMiQPn2" +
//				"iejBq+Ni6DZbxocXeQLaAMwurdFdBrZ/rOuXi9ud61HHmGGX0dbVtT114X/OiLFcuC6WPNZe13HiIO1naMbKz0J" +
//				"9Pf3M89+oKwF3O6s3LX1nutuVeJzQbYUBAVrzvEkAUC0yZ6TZikfbA0rafCA5P0XOu6kaj0mRoa8Qq8+1PTHUBA" +
//				"F0jk29p9rSGXkADxALIfMiF9HJIeJ5NBQ2W6ly8xgxgErBxcth7vl5SAiCsVGQKDa2bKDHIoJRkoqemMaeHmxkk" +
//				"JdFoU/joEknKe8PCq1csRt9beCN4y7qthGXTKloK/QZnqQAB9J2AuGRn/76q2pz0uKL7jzq8rtrsoOPvHM9ske9" +
//				"1rWFLWa7cvHxd92U5a1Diu/dONJ5/BXvnN+xCgBc80FR3vO8Ytu88A+cV7pDIz3s1zAGI8tScsMT09opv8uYH6e" +
//				"vaUqx0ETBdsf9DT2t8LUBUrP/9QO13ba1eAtkXWmPhZqz/TMWXiZ+wBgXJcP1/9SP+L1fY0AtVH3M9aLu8pj/6S" +
//				"sftJvhyR9pxZ5RZ2UDVxizXj76KHRRctvDWjP8/vOOFOfLnJtBR1cW9oosPJw7ay5I3p1jSwQ0mVxPr3duZXtfx" +
//				"/WmFPDqd33XfeAnBzWHa2j5eAGoB3NYEjNXemxf1xWN/nBIclFhlc9lBqFzxaKgTmoTCpyPj1eV4Arg91VqEGnR" +
//				"YEEOx9i7oxP+DJxi4AkIGU3RH5m1OM6SGUmJdEQwJ6UGXFBhiR8fnx+7F30vexSm3LvA9nDJCTBYuoN1iOFthh8" +
//				"r4XtVGNHbldITZQfozLKkL/HZLHkx5r/Ac2UUF9EWniblCRwTB+cdPitKYkP0yN+HSdv298PNL4q34SA80/suQO" +
//				"jSmRIgsWYbOuFJ26Jn4vFmWyrT7b9MSSrRRoTyeIfa+ahZkqQb3iaxSMlCRwzcpMPr7gj4SHaglzDt/Xdg+4lWu" +
//				"0wyTVISUnkFNvoEpdUyZSDfyhAgY3MvZKt5cJfpEMx5Z4vcAVF3qpSIlAlmWmthBiyVKJMYONdGbcrfT8iI7EZJ" +
//				"phF971tSPQ6tTonXhe+Gm+7o5nKVbMWdK8aoeCVgw30/dL631gcZt1LeTELtK75Df38eYOiDzLl2VHVQoFndl9c" +
//				"qLViBSMheP/f0h5Rq3v6oOS9MBYTQ3PoUnmg32cNwLe/tPyejUn4Yorr/qxLDtbm3wyOPsTIga22mZFDzcKQWRT" +
//				"8Sodo9kXBEYfcsqGc6RVsJiW/aiOcaYFtCpMjJ7wNcjN+wOyPgHKsRmHQyaUuh4QguLanplocWmXDo8340rB9Ak" +
//				"0EauVTo0wO3gphrqlq8vaTXWXS4bYaLaOU8nKkyhWDaEXkZdcrK+LiLTlWXUuTb41PfQ2GQX7Hj+3Hmo4pcjfG+" +
//				"yUtLfyUnrrjEsPqhXHUgUJEEtj4k8istNoVySg5PwYYOYzTK4fCPq8x1qRfDp4phogCqgUboUwcAUJZa0FphtV0" +
//				"XUQtgX6F5ywDiqI0k8Qor9fEZ3MKbeSO1RMt5+qwFlSbp2FxAY0gX1fdDURRZlyGwxPNxVrXJFqAaL/A+ByrKrk" +
//				"MTk0QkUAnhcIVVi2EPxTXLjnXw992eSsEX9JWwu7znrvxyP64VMtOQ4JLNAn3aGvsjJZ7HvxzZoz5pJ5qY8OAU4" +
//				"g9dXadKJhNYUimvwU5VHumYUFUw3KAiSZhi2S7O1AOAUQhnk2BYuTaf4gMCpgpldwLpe+0ZlI5H1XvpjAknY4QZ" +
//				"g0hpGsVB1950WMaK1kVUqtEznIJlUVhjTTVGPF7oRkGdTelOtU2e+lnYG25VLdS/fExmhVrchh5gvRYc6Vwj2bt" +
//				"7cSZMSZd+QaRZtgGQgipHkqlUtVCywGbqI3LIaxSyG9pUpeDSc5tUOpwYaBDoGi263E98NlHLcaAD0o+xgAg1De" +
//				"UsbiEcE8IDcUG9Ax6uQCv2rOHdPcqFAdlYy3AZUjPw4IhChQklYitSfqPV+fjBcDVTvXGiHdNVowxBcmJMzvwPt" +
//				"lTUZ+hVr5PJHAYUe6Q9bFG9zg9VCqWo7TOqByyY1m30m+hqMjBZHQm0JMDIWtZyFghqlF/YdECiuILRoayYIFwn" +
//				"IPXoT/LE+qOwWjJQc3lCMoNkTxPlNnkDoGeGwpOy2k0UrivxLoyayucPNooEI3J3DQ6O8lp5FniwmAD1ic2dydI" +
//				"DIiNYc8bZjJ16B97qEtyVoqVyE2r7RSIxEnFqBwSwk0N2Li1NXWiRPUYBTI+gLUGdsAsCgNjAeqDD4gP1wX6N3T" +
//				"5z8sUHYEnsi70EoIWiRo3uYFszCKoWUiqoAhdlk1+gTklyoSlZCfiKyN8MDjYWTk7zlyOy2yXoz7dyPNUt1DXYR" +
//				"/tKZGwSipXsfsYxObJEO9rkxthWbHmcvzzrgpjcqhs9mZhtdJyMFWpxyhUzHm9CVJKHa20mqCQLowtBSH/wTOY2" +
//				"6CLLBCS5x2kvnlLK36itFKjKwiK5Am0SwFNaYCwa5frHAP64zOKMS72D6UdQ1GQTb8YE54LMuNMozfHyHOfbaZE" +
//				"5fMzrS5JkFg1EKssT/2ueLDWu0gOsVAsB7dBOTRRWWpyCCAKowNWaybEj0NTUzo5ZcNzJvC8Bz4aDnUJGCN0xJu" +
//				"9ygPRSKxFSzkUVzcgkCXJUGRmhnJwBjmiFZ8dm6S9SPqm6CD9pH5x8GeAbaI724IMKUhjM0qQ2NRarpTZqIKHsj" +
//				"/wjXI4+sebf2Y2u/FylRKgthwodTkEuB4yJraae6AryKcj6Kg5+3C7UUWH6xoQr5oFEUx7oI565Na89WaodsX1C" +
//				"I5wUFDDVqkeNSeM7gqzXnMGf0WDHMb2TRGZyFtOt/673tbvqajrRTk7hHEf1cfSouOJvToJwqw1QXbFspDWODYn" +
//				"uQofS1sq2vTLwVHXC7k6/XJIVZeDKWAmVHlQvWYzDNhGpc6QsmY8HoCE4r0fuoYyDIyPdDULEikE5/T43V0OXIZ" +
//				"uejLsSGu2F0Lj7rnCRBJuAm3CPVVW7KLS/AMb1XtNH9lrxuq57aIjrX9hzHTeL78nO2KFXEcH00fsSk4ouxapWx" +
//				"Q+OYS3CETCyqFWnxwO65ADThQBex3kkqrkTHSklYWoFqMUM6TiYTn+5QvOQtGn4gj4E3eEY/QgdtiuwVlYbSeoN" +
//				"GArtcaU3Ksrdsgv0tRVjeu0rC5l10PPbWLCTW5BPl6VvR75sVOYHa/c/Wa8d7h1nlbdn/xcO6ahymMV2hRhAvNH" +
//				"9l4tN0iDCy5fri5T3sKwOD+alQMfp3jvcIsOOdQEyiGv92pdFoVa7EE2UQgGN0MiHQKBciPa4BF2eHIYxDI8kax" +
//				"lAxVXfrqyLq4rFhICvvDXDy7fy42gPjJVjElJXQ+FcgkWUw3eLzM3V22g3YWMsiqn5eh5p1DHyCyrK+/GiB1Cer" +
//				"nbayr4oOLzf5bXDpWr9/Gd1Y+9s8v9GD6LzhkV/adgSE85ZiRZ2VHAP4/VjmJOluP/Fnx6tjRY1zA/K4mVAx+zY" +
//				"eQw5v2KtV8dvyS7ee/x4vO0HJoeY7aac21UlQOPMDwopHaeglK2pd5yWsqHB/nobyJprR6Am4AoCDUF2K5T/7kU" +
//				"4ARdZxueKYmnondlL4bS0XdKwS78hLBg3RHxCpOOWRtiuzOR8dwZud27iEImvN8pmWVagbCFs0vlw2dDE+HeXT7" +
//				"lZ06pslWe9tgWnPZMY65LzLDs2HyPdSJf9s3b0p8sqDpTihq8TPkZiO7V17Y3gdd+bAZpyZbqjGhYVeb0Dm2t64" +
//				"xmTr7CSWLIgIiSh4b1/kZJ44MJcOPyVz+oh7Xk8YN67ErlJDKUlRkuB/asiXYElm5rBEtL3CA9ObxSKgfuPAptO" +
//				"eBEsXd+325xHDiOL748MroGof0iq8CInYz8xhoe82DqeSbp3O+m3ZtfAq4GBHNQMMIIMrj3NWqSCtIVvJYvNXto" +
//				"7x3vH29v81zotCOIbPg1DY3eXtLye3+bPWze9uo3DpezoQ0bezgN7bxUu9tBtZzlZQb26SOjX388v88/jMZPjFl" +
//				"3SDf5hgK8HI6Ut7NyYMlAIgfRMQm0uzIjX0MOEouiW0I00yODp2dD7MgbLHHrqjvb2WeN2IOwRuz8KedGrsHf4S" +
//				"r3vZqXAiF3bqUOX2bnfZk3Tf26cmt9pYfnE/bMSKX9CG9PTH9sS1Zt7qLDrZ9S9V4TF9vAj/azg3AIbH0jdj88M" +
//				"OKZvKxkt57nVXabFJXfzLtCOvrErlMP7HyMTQu8HF4+3PoZqvPywWAb1j47jODkMEC3HHzt6I7H13/x5Zeja03u" +
//				"/QGLJ/TnhJbw8bNmzSpWu/7zNWsKXZFtsjvpelPxN0+7994SQNAjUHzqUk6TFySmRcJTegf2wTN1iUcbqIKqZm/" +
//				"/9CjTqcxY08mJ16ecuNZkx8rB5S1gNo91VQ7d1qLgk7akloUhE5C3JvhYgMFXEhJceRT2T3EYvSY/O9mVD8Cma1" +
//				"12rByyQyOH7kkUyH8ylc/s8ccd9ET2hX37SFwnt+phJupHQGAM3fNNYbQy+5YsxevHwaRY+1cUIHlLGAHB1UQUu" +
//				"APhX/EwkM8AJWvgEHu/B3kvKQHB1UMUQuo1fx6FgTeRy2Vi/kuSkggICFFciVAFv+3TWJYcfsYmkBwPT9wPAgLj" +
//				"COu2BAGwpWN/gqWhswGEFRMEr9zeCgICYlFcHpyudtiwV/74DsZFvjUPve6H73SrwLTq0+cdNtLtBAQ9mCiOHj1" +
//				"q/suyV56Iz0qpEzLtRcfX6Vu5EMogP2nw9cRnptQx92DuRbqfgKCHuR7fbvx2+M+VJ25J6Z++HH9BMcTfQKo35R" +
//				"hJ3oUqxtpe/TPA17uLvBWOih+m3jn1CBkGBAQ9wKJY9sbyOY7Wi7lJqclvBOwsRfzBFBB4O72zo6KimrXqi4qOb" +
//				"qa81GyBMBASvViGqT8pNeUN5p7LXl8+hwwDAgINV/5KrgKUlZVZvtq2cX5Kv7Qquh1rhQ1gQpKUjyCYz4119XOz" +
//				"4tKOzZqhvs+Dx5r1awrPNFYPSkhJXikKbIpSwllKmll3+kLG3ZOmrhgyZEgHGRIEBN2MKJ5d9MLCzNy+L/ljEPx" +
//				"8L8rCnHnpzPmMyQUT/zFy5EiXkfoPHjyYuHXfjv9Kzk6toutbK3qBEfLfh/mp8hfH84uefXERGRIEBN2MKIqLi3" +
//				"OOnDs2PiIuahUA+Hs/uOPsKDS74XRN0jNPL3i7K/f562tL5iX0610PTfAzPnQhnJpF/9vW2PLQ8MxBOwsLCx1kS" +
//				"BAQdLMYBaOYka0mJuYwU/yyYwAaap3zk73RVV0lCQZMHUxdjbXOucJLgTiSoIlyZmSbuZmQBAFBN7UoePx56aKn" +
//				"eg3IOMe5BzPrHOczpoyb9EFeXp47lPf56aef4jb/sPXhlJx05rj3tTRbzKw9UZXxp/977m9kKBAQdHOiYMlixZK" +
//				"nUtJ6VbsddYkLnvrDO5fzXouXvTovvm9K/aXqmvSF8xcsI8OAgKCHEMWO77/vX19flzr9vul7/xX3W79hfUGSNe" +
//				"nChAkTTpFhQEDQQ4iCgICg+8JEREBAQECIgoCAgBAFAQEBIQoCAoJugP8XYADkzujuWY4GPQAAAABJRU5ErkJgg" +
//				"g==";
//		return out;
//	}

//	public static String getStormLogo() {
//		String out = "data:;base64,/9j/4QAYRXhpZgAASUkqAAgAAAAAAAAAAAAAAP/sABFEdWNreQABAAQAAAA8AAD/4QMraH" +
//				"R0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIen" +
//				"JlU3pOVGN6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZS" +
//				"BYTVAgQ29yZSA1LjAtYzA2MCA2MS4xMzQ3NzcsIDIwMTAvMDIvMTItMTc6MzI6MDAgICAgICAgICI+IDxyZGY6Uk" +
//				"RGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RG" +
//				"VzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG" +
//				"1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbn" +
//				"MuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3" +
//				"Rvc2hvcCBDUzUgTWFjaW50b3NoIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkQ4NzE1RTVGNDM5NjExRTJBOE" +
//				"Q1RkFDRDg5NzRDRTg2IiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkQ4NzE1RTYwNDM5NjExRTJBOEQ1RkFDRD" +
//				"g5NzRDRTg2Ij4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6RDg3MTVFNUQ0Mz" +
//				"k2MTFFMkE4RDVGQUNEODk3NENFODYiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6RDg3MTVFNUU0Mzk2MTFFMk" +
//				"E4RDVGQUNEODk3NENFODYiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eH" +
//				"BhY2tldCBlbmQ9InIiPz7/7gAOQWRvYmUAZMAAAAAB/9sAhAAGBAQEBQQGBQUGCQYFBgkLCAYGCAsMCgoLCgoMEA" +
//				"wMDAwMDBAMDg8QDw4MExMUFBMTHBsbGxwfHx8fHx8fHx8fAQcHBw0MDRgQEBgaFREVGh8fHx8fHx8fHx8fHx8fHx" +
//				"8fHx8fHx8fHx8fHx8fHx8fHx8fHx8fHx8fHx8fHx8fHx//wAARCABaASwDAREAAhEBAxEB/8QAtQAAAQQDAQAAAA" +
//				"AAAAAAAAAAAAQFBgcCAwgBAQABBQEBAAAAAAAAAAAAAAAAAQMEBQYCBxAAAgEDAgQDBQMHCgQEBwAAAQIDEQQFAA" +
//				"YhMRIHQVETYSIyFAhxQlKBkaGxsiMzwdFyknMkFSV1FmI0NTfwgqI2U5NUdEYXJxEAAgEDAgMEBwYGAgMAAAAAAA" +
//				"ECEQMEEgUhMXFBURMzYYGRIjIjBvCxQlIUFaHB0eFygmIkkjQW/9oADAMBAAIRAxEAPwDqnQAaADQAaAGHcm+tpb" +
//				"agaXNZOC0CivQzAufsUcdO27M58kI2VLmfqv2+Jzb7cxNzlX4hZSPTjPt+zVrY2K/c40OHdSIVkfqK7tXrMtpZ2m" +
//				"Mjb4Aw9RwPPx1c2fpWTVW17Rt3xol7nd57lxLPuKC1DUHpheR58BqYvpe2u2vqOPHZrTuP3jqXG5FVU4szpQBfM/" +
//				"b5a6f0va7/AOAK+xTY95O91r09GQtr7j1KkiAMy+2vIaZu/S0exoXxyTYn6oN62P8A7h26t1Avx3Fm3L8g4arL/w" +
//				"BNXI/Cdq8iydo/UX243E6W73bYy9c0+Xux0cfLq5apb+3XbfNDqmmWbDPDPGJIZFkjbiHQhgfyjUJo6M9IAaAAmg" +
//				"roAo3uN3/v4czc7d2PBFcXVk3Rkcxce9bxP4pGv32Hj4ausDaXcWqfIhZObG0QE9xO8Bl+Y/3UPV5+l8tF6P2dPT" +
//				"q3/Z7NKUKx7u68ifduO/WVnzVtt7e8EMM983p4/MW3uwSSeEci/cY+GqjP2rw1qhyLHFzo3R579793Ft3FYmDb90" +
//				"LG8yF78vLcFFkKoq14BgRz1H2zHhdm1Ieyrrtwcl2FS3PdLuximgu5dzfNxLcxJJA1vEAys9CK9Plq6u7XZjFuhV" +
//				"Y26yuzUaUOrrWcT26Sj76q39ZQ38usmy+Rt0AGgA0AU13535uvCZDb+J27fjGyZF5jcXHprIaR8AAGB1a7Xiwut6" +
//				"iDnZLsw1LsK0m7gd1okMk28vSjHN3giVR/6dXP7VY7imjvc26KNQte5fdZqyWe8orsoeKtbxOtf+Ki6R7VYfYdPe" +
//				"px+KLRZfa3vhkctm4tr7vtorXL3Ck46+tzSC66eJWh+F9VGftvhLVHjEtsPOjeXAdu/W+c3tjaUEuBuBa5C7vYbY" +
//				"XJUP0I4YtRWBFeGo+3WI3LumXKhIyLjhBtFQP3N7x42RbqLcMeR9Bqva3EMccbryPUwAIp9utBPaLLXBFJZ3nU+K" +
//				"p/Ewfun3VycjTR7sgiNer0LOCIons4gk6WG0WUjqe7TT+FrqKB3y7w2FubCe4x8yv/AA87NGEMY/CYxRS35NNS2W" +
//				"3q9A/DdFKPbq7hNB3O7tFxd227Y7k1+BreJoSR4UC8NP8A7PYa5DX7rJPjFoubtF3bk3aLjEZuCOy3JYKryxxn91" +
//				"PEeAmir7fiHhrP7hgOxL/iy2xshXY1RZmq4kBoANABoANACfI5KwxtnLe386W1rCpaWaQhVUD2nSxi26IDnXfv1G" +
//				"ZfMSXOM2HD6VlHVJ85N7qjwPpj9WtHt2wzu0chmd2hUcmDN3fC8zF42Ru5asWuGJ48y3QfDW2xdts2VwVWiNKbYu" +
//				"eOMLHHFGIoDxVSACyjyXy8dWMeHU5M5WZCxVJAvSCeR6qch9ns0RVe4DGNOmD3wrTMDI9TTpHtPhw0rdX6BAEkfS" +
//				"zOhdWKv0LWjEchQ8dFH2AehrqR7ggFTItJGbgAoHIU5DSUikvQKDCUpEvqqIhwRPh6z7B5aFSr4cQE2SxOOvERZI" +
//				"AQGotxTpckHj0U4mumZ2IXK61UK0Hbae9997CuxcYi5kyeJbjcY64JYhRz6CdZrcPp+M1WH9x6F1nSvbLvFtbfts" +
//				"VsnNrlYhW5xs3CRfMr5jWKycOdl8STGSZPNRDoYd85WTF7Tyt7FX1YLWZ46cwwXhru1GskvScyfA5B2xF6eCilHv" +
//				"zTB55CeJeRqnjrf21SKMjmTrcdRox2Iy0mBO7LW6nuZ7e9MeRsUJbojJPApqo/XSjkaJcIl3PFhK1RIVZjOW9/ae" +
//				"jZWl4b1JY5bSsDCkitUcfDU27k2pRa1IrcXCvW7ik6U7Scd0d8S7syWIw9tjrg3OFnilyl4xHpB3hXqFOYodVO1Y" +
//				"ri/E/Cyw3LJgrbi3R09pHtz291Pi2FpC1xKk0cohT4mVWqaV1d3Vqi0ZvAvRhdTk6Iv3tp3qx+4szFtqXDXeKvha" +
//				"+sjXDKyusQVTTp5ayWVt8rMdTaNhj5kLvwuo59wO9e2dn3qYoQzZbOuof/AA20ALIp5GVzwT7OeuMXBne4rgju/l" +
//				"QtKsmQ5PqUykb9d3sy6W28WiuEdwPPpKiupr2aX5iBHerDdNSLS2Tvzbm88T/iWEuPURG9O4t3HTNDJ+CRDyOqu/" +
//				"YlalSRaW7ikqopj6hane+0x/8Ac0/Pq22XnIqd7dLDKx3pbrNhUicBkkuoFKnkauBq8vS9x9DObRP566MV5rZ9tt" +
//				"Xe+PSzT0ospav6sS/D1IeBpqr2jIlNNSdaF7vkErNe4MpM9lfYLIRcLi0yduYiOfvMAdWOWtVqS9BUbNcfjU9BaH" +
//				"1KSmXbWLbzysJ/OG1ndp871GrzfKfQq3c6/wCSZOv/AMJv1jWtb4GLwpfMj1G2523bYO/2pdWiCJspZ9U4XgrUUG" +
//				"tPy6ptrvSlOabrRmn3SK8Gp7um0jumxNrIoeOa+iRlPIhjTjqyzZNWm13FXtL+Z6hU+Egwe885irZfTtohE6xD4Q" +
//				"WA5aj7RclK1VupL3dJND92/uns+7uBkjJBuLa4ikI8VCE/ya43tfJO9nlzXpOtIW6okbzAOsiXxloANABoA1Xd3b" +
//				"WdrLdXMgit4FMksjGgVVFSTpUm3RAcg9z+42Q7j5ua2tnkh2lYOwjRSVFwVNOpqeGtvsey8FOfL7yNcudgxRCJIo" +
//				"1iWGG3iX91GT7vV+IinhrZKCjwVfURzLj1IVZbhlRjI5HuVblxPE66/gICRUWjuTLMAoC1DELxPT7PadI33ckKaZ" +
//				"8hY25eS6uEiWoAErVNBw90V4fbrmc1FcWAhuN0YIqqC7jIB4CMM3GvNqezUdZtlfiF0s32+5sDLKoS9jCJwiiPul" +
//				"m/E7NpY5NuXBSVQ0sWpIsqIVMfy4JLktUOfCvPhqT94gAkmJupbh+pnkKiqhacqn8wGjv7ABEYDrkcCRgY4wtRxP" +
//				"H3Ps0rfYhAVlDn4umMBR1VYcPIAip0NcAEk8eSxuWt8/gn+VzdqfUUIxDSoOYcctVO47dG/B05/bgOQnQ6r7O90b" +
//				"Hf22lugRHlbSkeQtuRV/xAeR15pmYsrM6MmRlUk26rBb7C3NswrHNG8cg/4XFNRU6OorRx/jLW6wmSu9r5Aene2L" +
//				"s1qTwE0BNVZPPW4wslXbaaMtueO4S1djFFicxtzNPn9vEPK4pkMU/CK5j+8B5NrjMw43o0fMTC3Lw2oz5d/wDUv/" +
//				"t5e7S3lihlMK5SeMhbyyk92e3m8UceXk3jrIZGPK1KkjUW7ikqop3vFsf/AG5umzzVpczxnLXwW6g6v3Zovl419u" +
//				"rjacqUno7Eit3KzHw5NqroMO8JJ4sHcG3kaGQyIgkQ0YBmpwOry5KkWZTbkpXopqqLG2hsGHYtrdb2ubq4uruzsH" +
//				"kQ3D9SqDGGCjh4tTWUu5ly/SD7zbW8eFtViqEAwRnewnzl+xlyWUMl7eTNxY1qVWvkBrTW4KEVFdhh90yHdvuPYn" +
//				"QY9u5HcAyllLf3Jks8uszQRUoE9M0FDpm1k65yj+Usdz2+3bx1KKo0T7Y+cfa3czF3cZ6MfuCuPyMY4K0vOKQj8Q" +
//				"bx1G3O0p2m+2J19OZbdbb7OXRj935YSby2gwNai5/XqFs/ORZ7+6Y8uhXu7oLp8ShtoHuZIrmGUwx/EVRqmmruar" +
//				"Foym034wvpydFRizO7hvdz7qts3PYSYrD4m0aNPmWHW0jcWag8NRMDEdlOvNl1vG427sNFt6pSa5GO0LeLe+8Mbb" +
//				"41vVxGJnW7v7kigZlPuIqnieOudyylC3p7ZD2zbdO3JymqMsb6kEKbYxAIoTlID+dW1VbT53qLvO8p9Csd0D/JMn" +
//				"/ZN+sa1T5GHwX82HUUbu59vP8ATz+wuqTafMn1NZuvkPoNmd4XuD/1KD9vVrmeVLoU+0P5vqHbdf8A3Q3F/Qh/UN" +
//				"Rdm8n1k7eeaMto8O7G2f7K4/YOl3nyRdm5vqdb2f8Ay0f2ayJoTdoANABoApH6pd13Njtez25YzGO7zcwSYJ8XoL" +
//				"8X59XOy4njXkhu5KiKMS0itbdLeEHpKrH0qAPcA40+069QtpJJLlEhMyfrMYAgR2HT6aeXT5k/yDXapXmIeNC7eo" +
//				"0/SikVLAEKKcST7Boc0lwFoe7X2xu7uFlmsNrxtBioSEuMtIKcB5N5eQGsvuW+K2qRY9C1UlWT212M2DItpmJZt1" +
//				"7kT+Pbwksiv+FqV1nY3crJlWPaO+7Ezi7s7Fsnjew7bxhadMrOoJA0/wDsuY1x1CeJEkW2s59O2+bgYy+wceDybm" +
//				"ixTL6fUx8n1Av2cqxzqdpxZo3v9MORxyy5DY14XjHvnFzmqkc6LXhqVgb/ADttVZzK0mVXZXc1zc3FrfQfKZG0/d" +
//				"XdoRRlYGgIH4dbzCzYXoVjzI0o0YsEatKkRZlhjUVk8STzC15DUyvCvacBKDJExQdC9Sx8K8z8X5dC4PiB60qujI" +
//				"y++wX01FW6SONXbyA0KNGA49tdyTbP7mYvIQsUxmeb5W+j5L11p1U/TrI/UmEmtRIsyOzGVXQqeKsNYElFOd4+08" +
//				"eetUvLP9xlLQl7K8X4lPPpanNTqViZcrMqrkNXbSmqMpKxvr+O+kxGZhNpmoBVozwWVR9+M+Othj5Mb0axMjnYLs" +
//				"uv4Rbj83fbQ3DbbuxZKiBlTM2q8FuLZjRupfFl0zm4yuwa7R3a8xwmoPk+X9CefUFkbPJYnbGRs5BLaXF9HNDIPF" +
//				"JY6rqi2lUvNMvtx42X0K23j/0WY/hmiJ/r60V34WY/a/PiXj3bkYdm8l6Z6eqwgLU/CRHUayGJ50epu7nwFOpT/b" +
//				"46eA+SNP8A5Z1rzzWXnf7/AMxt9JI8NsCcfxHW5Rvs6zqpxH8+4bDeFXFfQW7if03w1wvBoMnAwPl7w1NyFW3LoZ" +
//				"vYHTI/1Jr3pfq3Zs5udRcfr1U7Rzkab6g/9aXQhe6cnfY7E/MWBVbppo4kZxUDrNOWrqTom+4x22Ysb15Qn8NBJc" +
//				"Pu/F5Wwtc96FxZZNmiXoXiGA8j4ai4mbG82kqUL3P2W3YtOcKqSFsN4u0c5jdzYmNbeWC6jhvoo/djlglPSwdeWn" +
//				"suwrltpjWy5tzxdEnVPvLK+pSaObbWJkQ+62Ttyv8ARKsR+jVFtPneo1Wd5T6FYbo/6Jk/7Jv1jWpfIw2D5sOpv3" +
//				"dz7ef6ef2F1S7T5k+prN28h9Bsz3/O4P8A1KD9vVrmeVLoU+z+b6h23V/3Q3F/Qh/UNRdm8n1k/euaMtpf92Ns/w" +
//				"BlcfsHS7z5Iuy831Ot7P8A5aP7NZE0Ju0AGgA0AcwfUk0zdzsEsjfuktnMAp96h/PrXfS6Wv1MYvciASLcSOCXbi" +
//				"oYkEA/YTyUa3yaREPQXCu5Yuag1BAHsFfADSCjPu2W4jwZiViBNIqO1TwSRuJ/LTUHcZUtNo6hzOjt1Onb/wCn4N" +
//				"ttfRla1iHzMIqeqZatISNeaW/m5Hvd5MfBHNxQ4jCx30MPzeRnpJLdMeokSc3alSeJ16SlHHs+4u72+n0EN8WIJ9" +
//				"w5uwEck0kc0fWA8DJ0N51Xiaj26ayL9yylOTi19uQqSY9Ziw/xKyWZgVuFjM8EwAWSNlFQKjkPOupeRYhdttNHKb" +
//				"TOp/p/3Rktx9ssbeZFzJdw9Vu8p5sIuAJ/JryrPtKF1pE6Lqipfqc2/aYneuD3BZAQXGUDW9508A5HAMdaD6ZyJK" +
//				"5p7Bq8uBAWNGeV5j0KOocK1A5cPbr0Bd1CKDxy06ipaR3DxKTQD8TEeehNeoQCPSjAdvec8E5VqfiJFfs0nN8BRs" +
//				"3OLhre0mRqyw3sXo8loSRxpqr3WMfAZ3DmdqwZqPFbNjy+Wbojs7MT3TJ7x6USpp5nXlrhqnpXaya3RVKwf6odlz" +
//				"wE/wCDZWSBhUOIEoV8/j1Y/s130EV5kE6VI13Vx2D3VsCLfeADI1mPm7S4I6XVVbpkif8AmrrjElOxf0vozq9CNy" +
//				"DryINHMl5ifVcDoubYs6+HvJU/p1rjEyg4XKLskeZG7mm7SbMWVi3pZAxoTz6FZunWfxkv1cjW5XGw+jE+8T/klx" +
//				"/aR/t6urq91mU21fOiXn3UtZbntBkIouLjHwvQeSKjEfm1jsWVLyfpNxcXuFL286zbdSSM+7JZHpp/QI1saHnc7e" +
//				"m9R/n/AJiAyh8VsCAckiupD9ofVThx+fcNXuz/AOt6hZmgbjIbfsl4vc5OGg9isCTqZlulqT9BRbFa+c36Ca96fd" +
//				"3Rs77Lg/p1U7NzkaDfVXHl0ILu1gcbbg+N7b/tjV1eXuPoZrZo0yF0ZO+98EUN5s51UKPmpQSP6I1RbN8b6Gq3hV" +
//				"sS6ED3gx/wGX+2h/b1obi91mV2qP8A2I+ssfv4xOzMFUUIvrOtf7LWZ2rz/UzaZvlPoV7uc/5Jkv7Jv1jWra4GJw" +
//				"o/Nh1FO7+fbz/Tz+wuqPafMn1NVuvkvoNedP8AfcH/AKlB+3q2zfKl0KjaF831Dvus/wD9Q3F/Qh/UNRdm8knbyu" +
//				"KPdo/919s/2dx+wdG9eSLsy4s64s/+Wj+zWRNAbtABoANAFE/VTtu6lwWN3TZxepJhZh8yAPe9F+B5eGrvY8rwry" +
//				"G7kaopS1KXscV3EALVlrGAQRx5k8eLa9PVxU4OtSFQ2r0q1er01j+CJqMFB8T5k6Xn6QE9/b2l1G1lcyGUSIfWPI" +
//				"8TUMAOXT4a5na1waa4ME6D5gO6m4sHtC72huPHHNYK4ia3sZ2NGTq4IGJ5fl1iM3Y5wuqUCTG7w4kRsduXUWOazu" +
//				"Z5VR+LBHXpjjrXoB4kjWnsYjdnROT6IYk+NUZ222cJaXHrXUjuI6MjXDAD2cD4a6hhW7dJc2u91oDk2YxJlt3XyY" +
//				"Ha9rJcSSMUnuEBKANwJ6vKmom47tC3FpPj3nUINs7O7c7PtdmbMx+DjYf3WPquJDyMjcXOvNsi87k3ImJURzf3v3" +
//				"vabu7jW9jYus2L28GBccVknPxUI8tbD6Z2+XG4+wj3p9hGqyOS/S7jqJaRgOpvwhFFAqjzOtpwXAjmI61MbmI+oY" +
//				"24swABbm3Mk8PyaO9V7QPVWMyL7xCEH36cx49Nf16G3QBl3FIbhLCyt0oby9jVTWrOARxXxpXx1WbxLTZafM7t8z" +
//				"szK4QybEGIckpJZfLy9XE++nj9h15Y50nqXeTWqqhybjJp8RcTbbyzehf2LNGiye6ssR+FlJ4Hhrb4eRG7BNGYz8" +
//				"RxnqS4GQwl4MZPh4svdRbfuZPWlxiuPSLVrwNeA0Swrbnrp7wLcrijShjkrwGGPA4cevk7sLa28EXvdCngWankNL" +
//				"kXo24NsZxMSVy5qa4Vr1LI3526vrPthjrTGRma5wRiuAgFTJ0Amantq1dZTDy0r+uX4jS37Oq3pK7ucjg87jZbZr" +
//				"tYvXUdaseiSNxx5NTkda3hJGRjZu2JqVORYHazd25Mxkb7ZebySZnFwY2tvL0r1haiPpLLz9xvHWb3TEhaSlHnU0" +
//				"2DkSuxq1T0EDKSbRydxtjMgxQRuxxt04PpyQOfh6vZq3wcqN2C7yl3Tbpa9cV1/qIsfjcXi7pbybMLNbWwcWcDup" +
//				"WJZDVqUJ1IjZjFtrtI+Rk3b0Fb0Mm3abCT7q3tFnzEww2IVksGcEetO/AuAfBeeqbd8pU8OPrLjaMF2o1lzY99/h" +
//				"Hbbq2gZGEagXFWcgDn5nTWyfFId3iLdlpFb7mvLSWxtkjnjdzeW9FV1Y/GPAHV7fXuPoZ/a7UleTafIuPvjtzIZD" +
//				"aVlfWERmvMROl2sQFS6dIVwKeIHHWV26+rd2r5M1uXZ8S24lN3uQwefxsln88LV5CrHqPRJHIpryanI613CSMfas" +
//				"3ce4paa0FOW3ZmM124gjzF789Ljs7FbxXbUBMSR+7Ujhw1TW7EbeWlFUWk00pudht938jXuS+s3w2RVbiJmaJgAH" +
//				"Uk8Ry46vZLgZnEsyVyPB8xx3qyRJ29eRgi/wCHn3mIA+BfE6odo8yfU0m5Ktqi7hny1zbS5HBpHNG7f4lBwVgx+M" +
//				"eR1bZ3lS6FXtkGrnFDzvOaGDujuESyLHVIadbBa+6PPUXZfJJm7Rbaoe7Kmgm7r7b9KRJKR3Fehg1P3beWjevJF2" +
//				"mLTdTr20H92j/ojWQLw26ADQAaAE2Sx1nkrCewvYhNa3KGOaJhUFWFNdRk4uqA5D7j9rMz23ykl3awyZHaEzlx0A" +
//				"l4Orw4cqa2mz746aJEa5bI9Y3yX9oZLJ1kjJBEa0Z1APEyE8eGtlbuwnxTqR2mZz3YgYtxeSVwlrZxrWadz4jTOV" +
//				"kwswrL7egWMWy19k/TdJnLA5De800HzA67XFQuV9INxBkPi2sDuG/3Lkvd5IlQtJC+X6QtrmVjDnb+OEnhHUHh5V" +
//				"rqNHfbyVP5nXhIWW30lbCS7E97fXt7EKH0pHoDTzNTpqe83mqB4aJfPn+03bHF/LwyWlgsYp6EHS0zU/FTj+fUTR" +
//				"dvOr4nXBFH9xu92697s+N24smK28fde4HC4nB8uXu6021fT7dJXOCGZ3SF2NnDZRNDbjphQAsSOqSaQ8yza21qzG" +
//				"3FRiv7EZurFDBShDqXlkb3qPUqpH5PDTi5+hAD/KhgAxV2AUuwJAoPZWtPLSrUIexoWlMo91BRVkmI4jl1Ee3y0j" +
//				"dFT7hR97KbTbefdEZOX99htv8Av+p0+48o+FR7K6xX1FnqmiP27yRZgdfuiuhRhVSKEaxJJK4332gwG5FDXdoszp" +
//				"X05R7si/Yw46dtX523WLocyinzK6P00YsydKy3vR+ETHl+bUz91v8AeNfp4dxPNjdltvbdl9a2sxHMQOudyXlI8u" +
//				"puOol3Inc+J1HYwS5FiXOJtJ7b0CtBSgOmTorLP9jNm39y9xcWFsJWNWeojJ+2hGnoZFyPCLZw4J9g4bM2Dsbak0" +
//				"klmbKzmlHTLIJkLso+7Uty13N3rnPVIFGK5G/d22e3+djMd/eY6VPwzTRcD5g9XDRCzei6qMl6mK6PmQi37P8AaG" +
//				"K4Dm5xTdJqFa6Rh+bq0+/1b7J+xnGiBZe35tj4e3SG2ydiojFFWOaIKoHkAdM/o77/AAS9jO9SGjfOD7d7uiRMne" +
//				"Y+5EJLQ+pOoKE8+kqfHXduxkQfuxkvUJLS+ZE8R2m7SWN9FdGbGM8TB4+q4BAINQaFtOuOW1RqfsZzogWbLmNlzW" +
//				"ny8mZsSaULG4iHH+tqL+kvfkl7GOakQHMdr+1+WuTO0mLnkY1ZlniVj9pVhpyEciPJTXqZy4xYtTtjsA4M4WNLE2" +
//				"Dt1m1WZCDIPv16q9WuXK+panq1d4aY0oNlp9PGyDMJP8OgljBqFEtR+htH6y9+ZieHHuJPuPtLt/N2Nva39hHcR2" +
//				"i9FqpqPTWlKLTw4aahfnBtxdKnTgmMmE7BbQx99Fdrio/WhYPE5LHpYGoNCfDXcsu61RyYnhx7hduzsttvcF789f" +
//				"Y9J71lCtcEkMQOQNDpLeTcgqRbSFcEzZs3sxtnbuQF9Z46OG7UFVuKksAeYFfPRcybk1STbQKCRZSqFUKOQFBpg6" +
//				"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";
//		return out;
//	}

	public static String getMiltonLogoStyle() {
		String out = "#miltonlogo { width: 150px; margin-left: 15px; position: absolute; bottom: 8px; right: 15px; }";
		return out;
	}

	public static String getH1Style() {
		String out = "h1.title { float: left; font-size: 22pt; padding-top: 10px; padding-left: 5px; }";
		return out;
	}

	public static String getStormLogoStyle() {
		String out = "#stormlogo { width: 180px; float: right; padding-top: 5px; }";
		return out;
	}

	public static String getEntryListStyle() {
		String out = "table {width: 100%; font-family: Arial,\"Bitstream Vera Sans\",Helvetica,Verdana,sans-serif; color: #333;}";
		out += "table td, table th {color: #555;}";
		out += "table th {text-shadow: rgba(255, 255, 255, 0.796875) 0px 1px 0px; font-family: Georgia,\"Times New Roman\",\"Bitstream Charter\",Times,serif; font-weight: normal; padding: 7px 7px 8px; text-align: left; line-height: 1.3em; font-size: 14px;}";
		out += "table td {font-size: 12px; padding: 4px 7px 2px; vertical-align: top; }";
		out += "img {margin-right: 5px; margin-top: 0; vertical-align: bottom; width: 12px; }";
		return out;
	}

	public static String getNavigationTableStyle() {
		String out = "table.navigator { border-bottom: solid lightgray 1px; margin-bottom: 16px; margin-top: 20px; clear: both; }";
		return out;
	}

	public static String getNavigationTdStyle() {
		String out = "table.navigator td { font-size: 14pt; padding-bottom: 8px; }";
		return out;
	}
}