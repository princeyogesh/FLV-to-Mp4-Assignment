package com.example;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.bytedeco.javacpp.*;
import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.*;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.swscale.*;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;


public class ReadFewFrame {

    static class StreamContext {
        AVCodecContext decoderContext;
        AVCodecContext encoderContext;
    }
    static StreamContext[] streamContexts;

    static AVFormatContext inputFormatContext;
    static AVFormatContext outputFormatContext;
    

    
    public static void main(String[] args) throws Exception {

        //File curDir = new File(".");
        //getAllFiles(curDir);
        //Doing initial setup here, initializing logger, defining input, output paths
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        Logger logger = Logger.getLogger("video processing"); 
        av_log_set_level(AV_LOG_DEBUG);
        final String inpFile = "flvmpconverter/src/main/resources/bun33s.flv";
        final String outFile = "flvmpconverter/target/out.mp4";

        //Create new contexts one for input, other for output
        int res;
        inputFormatContext = new AVFormatContext(null);
        outputFormatContext= new AVFormatContext(null);
        res = avformat_open_input(inputFormatContext, inpFile, null, null);
        if (res < 0 ) {
            logger.log(Level.SEVERE, "ERROR in opening " + res);
        }
        res = avformat_find_stream_info(inputFormatContext, (PointerPointer)null);
        if (res < 0 ) {
            logger.log(Level.SEVERE, "ERROR in finding streaminfo ");
        }

        res = avformat_alloc_output_context2(outputFormatContext, null , null, outFile);
        if (res < 0 ) {
            logger.log(Level.SEVERE,"ERROR in allocating output context ");
            
        }
        streamContexts = new StreamContext[inputFormatContext.nb_streams()];
        int[] stream_list={-1, -1};
        int index = 0;
        for(int i = 0; i < inputFormatContext.nb_streams(); i++)
        {
            logger.log(Level.FINE, "hello there are only two streams in input video");
            
            AVStream currentStream =  inputFormatContext.streams(i);
            AVCodecParameters currStreamParameter = currentStream.codecpar();
            if( currStreamParameter.codec_type()  == AVMEDIA_TYPE_VIDEO ||
                currStreamParameter.codec_type()  == AVMEDIA_TYPE_AUDIO)
            {
                logger.log(Level.INFO, "processing stream "+ currStreamParameter.codec_type());
            }
            AVStream outStream = avformat_new_stream(outputFormatContext, null);
            //here creating new stream might fail but don't know 
            //how to detect creation of new stream is failed or not
            
            //copy codec parameters of currentInputStream to new outputStream
            //append new outStream in stream_list to add all streams in single container ie mp4
            avcodec_parameters_copy(outStream.codecpar(), currStreamParameter);
            stream_list[i] = index++;
            
            
        }

        ///////////////////////////////////////////////////////////
        //now write streams to files
        AVIOContext pb = new AVIOContext(null);
        if (!((outputFormatContext.oformat().flags() & AVFMT_NOFILE) > 0))
        {
           res = avio_open(pb, outFile, AVIO_FLAG_WRITE);
           outputFormatContext.pb(pb);      //Very bullshit part need to be done
           if(res != 0)
           {
                logger.log(Level.SEVERE,"Something went wrong while writing file "+ res);
           }
        }

        res = avformat_write_header(outputFormatContext, (PointerPointer)null);
        AVPacket pakt = new AVPacket();
        int pkt_num = 1;

        //copy packets from input to output 
        while (true) 
        {
            AVStream iStream, oStream;
            logger.log(Level.FINE,"processing packet "+ pkt_num++ );
            res = av_read_frame(inputFormatContext, pakt);
            if(res < 0)
            {
                logger.log(Level.FINE,"BREAKING");
                break;
            }
            iStream = inputFormatContext.streams(pakt.stream_index());

            pakt.stream_index(stream_list[pakt.stream_index()]);

            oStream = outputFormatContext.streams(pakt.stream_index());
            pakt.pts(av_rescale_q_rnd(pakt.pts(), iStream.time_base(), oStream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
            pakt.dts(av_rescale_q_rnd(pakt.dts(), iStream.time_base(), oStream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
           // pakt.duration(av_rescale_q(pakt.duration(), iStream.time_base(), oStream.time_base()));
            //pakt.pos(-1);
            
            /////////////////////////////////////////////////////////
            res = av_interleaved_write_frame(outputFormatContext, pakt);
            if(res < 0)
            {
                logger.log( Level.SEVERE,"Something went wrong in loop");
            }
            av_packet_unref(pakt);

        }
        av_write_trailer(outputFormatContext);

        
    }


    //helper function for getting all files in CWD not using currently
    private static void getAllFiles(File curDir) {

        File[] filesList = curDir.listFiles();
        for(File f : filesList){
            if(f.isFile()){
                System.out.println(f.getName());
            }
        }

    }
}