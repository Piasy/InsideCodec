def read_data(file):
    with open(file, 'r') as f:
        data = f.read()
    lines = data.split('\n')

    update_br = []
    last_frame_ts = 0
    for line in lines:
        if 'reportEncodedImage' in line:
            last_frame_ts = int(line.split(' ')[8])
        if 'update bitrate' in line:
            br = int(line.split(' ')[7])
            update_br.append((last_frame_ts, br))

    config_line = [line for line in lines if 'Test config' in line][0]
    test_name = config_line.split('{')[1].split('outputWidth')[0]

    encode_stats = [line.split(' ')[6:9] for line in lines if 'reportEncodedImage' in line]
    encode_stats = [[int(stat[0]), int(stat[1]), int(stat[2])] for stat in encode_stats]

    i_frames = [(stat[2], stat[1]) for stat in encode_stats if stat[0] == 1]
    all_frames = [(stat[2], stat[1]) for stat in encode_stats]
    return test_name, i_frames, all_frames, update_br

def calc_bps(bps, all_frames):
    for frame in all_frames:
        ts = int(frame[0] / 1000000)
        if ts in bps:
            bps[ts] = bps[ts] + frame[1] * 8
        else:
            bps[ts] = frame[1] * 8

def plot(test_name, i_frames, all_frames, update_br):
    import matplotlib.pyplot as plt
    from collections import OrderedDict

    bps = OrderedDict()
    calc_bps(bps, all_frames)

    plt.xlabel('ts')
    plt.ylabel('bps')

    x = [t for t, b in bps.items()]
    y = [b for t, b in bps.items()]
    plt.plot(x, y, 'r-')

    x = [int(t / 1000000) for t, b in update_br]
    y = [b * 1000 for t, b in update_br]
    plt.plot(x, y, 'b.')

    plt.title(test_name)
    plt.show()

if __name__ == '__main__':
    import os
    test_name, i_frames, all_frames, update_br = read_data(os.sys.argv[1])
    plot(test_name, i_frames, all_frames, update_br)
