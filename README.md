Source path = flvmpconverter/src/main/resources/bun33s.flv
target path = flvmpconverter/target/out.mp4

flow of the code is 
there is class named ReadFewFrame which does the conversion from flv to mp4.

followed steps in sequence as 
create two contexts one for input & other for output

open the input file and find number of streams in file like audio, video & subtitles, give file has only audio and video

for each input stream create corresponding output stream

copy codec parameters directly since no transcoding is needed from input stream to output stream.

open the output file and write header with help of outputContext

start loop over reading input file packet, copy those packets to output file
