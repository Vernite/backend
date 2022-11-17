package dev.vernite.vernite.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;

public class ImageConverter {

    /**
     * converts any video/image/picture to webp. Default settings: 75% quality, lossy, YUVA420P
     * 
     * @param b
     * @return byte array
     */
    public static byte[] convertImage(String filename, byte[] b) throws IOException {
        Pointer mem = null;
        AVIOContext pb = null;
        AVFormatContext ifCtx = null;
        AVCodecContext decCtx = null;
        AVFrame src = null;
        AVFrame dst = null;
        AVPacket pkt = null;
        SwsContext swsCtx = null;
        AVCodecContext encCtx = null;
        BytePointer data = null;
        try {
            mem = avutil.av_malloc(b.length);
            if (mem == null || mem.address() == 0) {
                throw new IOException("Could not allocate memory");
            }
            mem.capacity(b.length);
            data = new BytePointer(mem);
            data.put(b);
            pb = avformat.avio_alloc_context(data, b.length, 0, null, null, null, null);
            if (pb == null) {
                throw new IOException("Could not allocate AVIOContext");
            }
            ifCtx = avformat.avformat_alloc_context();
            if (ifCtx == null) {
                throw new IOException("Could not allocate AVFormatContext");
            }
            ifCtx.pb(pb);
            if (avformat.avformat_open_input(ifCtx, filename, null, (AVDictionary) null) < 0) {
                throw new IOException("Could not open input");
            }
            int stream = avformat.av_find_best_stream(ifCtx, avutil.AVMEDIA_TYPE_VIDEO, -1, -1, (AVCodec) null, 0);
            if (stream < 0) {
                throw new IOException("Could not find video stream");
            }
            AVCodec decoder = avcodec.avcodec_find_decoder(ifCtx.streams(stream).codecpar().codec_id());
            if (decoder == null) {
                throw new IOException("Could not find decoder");
            }
            decCtx = avcodec.avcodec_alloc_context3(decoder);
            if (avcodec.avcodec_parameters_to_context(decCtx, ifCtx.streams(stream).codecpar()) < 0) {
                throw new IOException("Could not copy codec parameters to decoder context");
            }
            if (avcodec.avcodec_open2(decCtx, decoder, (AVDictionary) null) < 0) {
                throw new IOException("Could not open decoder");
            }
            src = avutil.av_frame_alloc();
            dst = avutil.av_frame_alloc();
            if (src == null || dst == null) {
                throw new IOException("Could not allocate frame");
            }
            pkt = avcodec.av_packet_alloc();
            boolean done = false;
            while (!done && avformat.av_read_frame(ifCtx, pkt) >= 0) {
                if (pkt.stream_index() != stream) {
                    continue;
                }
                if (avcodec.avcodec_send_packet(decCtx, pkt) < 0) {
                    throw new IOException("Error while sending a packet to the decoder");
                }
                avcodec.av_packet_unref(pkt);
                while (avcodec.avcodec_receive_frame(decCtx, src) >= 0) {
                    done = true;
                    break;
                }
            }
            dst.width(400);
            dst.height(400);
            dst.format(avutil.AV_PIX_FMT_YUVA420P);
            swsCtx = swscale.sws_getContext(
                    src.width(), src.height(), src.format(),
                    dst.width(), dst.height(), dst.format(),
                    swscale.SWS_BICUBIC, null, null, (double[]) null);
            if (swsCtx == null) {
                throw new IOException("Could not initialize the conversion context");
            }
            if (swscale.sws_scale_frame(swsCtx, dst, src) < 0) {
                throw new IOException("Error while converting");
            }
            AVCodec encoder = avcodec.avcodec_find_encoder_by_name("libwebp");
            if (encoder == null) {
                throw new IOException("Could not find encoder");
            }
            encCtx = avcodec.avcodec_alloc_context3(encoder);
            if (encCtx == null) {
                throw new IOException("Could not allocate the encoder context");
            }
            encCtx.time_base().num(1);
            encCtx.time_base().den(1);
            encCtx.width(dst.width());
            encCtx.height(dst.height());
            encCtx.pix_fmt(dst.format());
            if (avcodec.avcodec_open2(encCtx, encoder, (AVDictionary) null) < 0) {
                throw new IOException("Could not open encoder");
            }
            if (avcodec.avcodec_send_frame(encCtx, dst) < 0) {
                throw new IOException("Error while sending a frame to the encoder");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (avcodec.avcodec_receive_packet(encCtx, pkt) >= 0) {
                BytePointer d = pkt.data();
                byte[] bytes = new byte[pkt.size()];
                d.get(bytes);
                baos.write(bytes, 0, bytes.length);
            }
            // flush packet
            if (avcodec.avcodec_send_frame(encCtx, null) < 0) {
                throw new IOException("Error while sending a frame to the encoder");
            }
            while (avcodec.avcodec_receive_packet(encCtx, pkt) >= 0) {
                BytePointer d = pkt.data();
                byte[] bytes = new byte[pkt.size()];
                d.get(bytes);
                baos.write(bytes, 0, bytes.length);
            }
            if (baos.size() == 0) {
                throw new IOException("Could not encode");
            }
            return baos.toByteArray();
        } finally {
            if (encCtx != null) {
                avcodec.avcodec_free_context(encCtx);
            }
            if (swsCtx != null) {
                swscale.sws_freeContext(swsCtx);
            }
            if (pkt != null) {
                avcodec.av_packet_free(pkt);
            }
            if (dst != null) {
                avutil.av_frame_free(dst);
            }
            if (src != null) {
                avutil.av_frame_free(src);
            }
            if (decCtx != null) {
                avcodec.avcodec_free_context(decCtx);
            }
            if (ifCtx != null) {
                avformat.avformat_free_context(ifCtx);
            }
            if (pb != null) {
                /* note: the internal buffer could have changed, and be != avio_ctx_buffer */
                avutil.av_free(pb.buffer());
                pb.buffer(null);
                avformat.avio_context_free(pb);
            } else if (mem != null) {
                avutil.av_free(mem);
            }
            if (mem != null) {
                mem.close();
            }
            if (data != null) {
                data.close();
            }
        }
    }
}
